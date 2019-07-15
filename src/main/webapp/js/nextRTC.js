'use strict';
//require('webrtc-adapter');

/**
 * NOTE: custom modifications made by Pablo Fabricio Lettieri (fabri1983@gmail.com)
 */
var NextRTC = function NextRTC(config) {
    if (!(this instanceof NextRTC)) {
        return new NextRTC(config);
    }
    var that = this;
    this.mediaConfig = config.mediaConfig !== undefined ? config.mediaConfig : null;
    this.type = config.type;
    
    this.signaling = new WebSocket(config.wsURL);
    this.peerConnections = {};
    this.localStream = null;
    this.signals = {};
    this.channelReady = false;
    this.waiting = [];

    this.call = function(event, data) {
        for (var signal in this.signals) {
            if (event === signal) {
                return this.signals[event](this, data);
            }
        }
        console.log('From ' + data.from + '. Event ' + event + ' do not have defined function');
    };

    /**
     * This function sends a message over the websocket.
     */
    this.request = function(signal, from, to, convId, custom) {
        var req = JSON.stringify({
            signal: signal,
            from: from,
            to: to,
            content: convId,
            custom: custom
        });
        
        if(!this.channelReady){
            if(req.signal !== undefined && req.signal !== ''){
                this.waiting.push(req);
            }
        } else {
            console.log("req: " + req);
            this.signaling.send(req);
        }
    };

    /*###################################################################################
     * Websocket events 
     *###################################################################################*/
    
    this.signaling.onmessage = function(event) {
        console.log("res: " + event.data);
        var signal = JSON.parse(event.data);
        that.call(signal.signal, signal);
    };

    this.signaling.onopen = function() {
        console.log("channel ready");
        that.setChannelReady();
        that.onReady();
    };

    this.signaling.onclose = function(event) {
        that.call('close', event);
    };

    this.signaling.onerror = function(event) {
        that.call('error', event);
    };

    /*###################################################################################*/
    
    this.setChannelReady = function(){
        for(var w in that.waiting){
            console.log("req: " + w);
            that.signaling.send(w);
        }
        that.channelReady = true;
    }

    this.preparePeerConnection = function(nextRTC, member) {
//    	if (Object.keys(nextRTC.peerConnections) > 2) {
//    		console.error("Max number of 2 participants reached.");
//    		return null;
//    	}
    	
        if (nextRTC.peerConnections[member] == undefined) {
            
        	// compatibility for firefox and chrome
        	var pcPrototype = window.RTCPeerConnection || window.mozRTCPeerConnection || window.webkitRTCPeerConnection || window.msRTCPeerConnection;
        	var pc = new pcPrototype(config.peerConfig);
            
            pc.onaddstream = function(evt) {
                nextRTC.call('remoteStream', {
                    member : member,
                    stream : evt.stream
                });
            };
            
            pc.onicecandidate = function(evt) {
                handle(pc, evt);

                function handle(pc, evt) {
                    if((pc.signalingState || pc.readyState) == 'stable'
                        && nextRTC.peerConnections[member]['rem'] == true){
                        nextRTC.onIceCandidate(nextRTC, member, evt);
                        return;
                    }
                    console.log('Candidate not ready. Retrying in 2 secs...')
                    setTimeout(function(){ handle(pc, evt); }, 2000);
                }
            };
            
            nextRTC.peerConnections[member] = {}
            nextRTC.peerConnections[member]['pc'] = pc;
            nextRTC.peerConnections[member]['rem'] = false;
        }
        
        return nextRTC.peerConnections[member];
    };

    this.offerRequest = function(nextRTC, from) {
        nextRTC.offerResponse(nextRTC, from);
    };

    this.offerResponse = function(nextRTC, signal) {
        var pc = nextRTC.preparePeerConnection(nextRTC, signal.from);
        pc['pc'].addStream(nextRTC.localStream);
        pc['pc'].createOffer({offerToReceiveAudio: 1, offerToReceiveVideo: 1})
            .then(function(desc) {
                pc['pc'].setLocalDescription(desc)
                    .then(function() {
                        nextRTC.request('offerResponse', null, signal.from, desc.sdp);
                    }, that.error);
            });
    };

    this.answerRequest = function(nextRTC, signal) {
        nextRTC.answerResponse(nextRTC, signal);
    };

    this.answerResponse = function(nextRTC, signal) {
        var pc = nextRTC.preparePeerConnection(nextRTC, signal.from);
        pc['pc'].addStream(nextRTC.localStream);
        var sdpPrototype = window.RTCSessionDescription || window.mozRTCSessionDescription || window.webkitRTCSessionDescription || window.msRTCSessionDescription;
        var sdpWrapper = new sdpPrototype({
            type : 'offer',
            sdp : signal.content
        });
        
        sdpWrapper.sdp = setBandwidthSettings(sdpWrapper.sdp);
        sdpWrapper.sdp = setAudioSettings(sdpWrapper.sdp);
        
        pc['pc'].setRemoteDescription(sdpWrapper).then(function() {
            pc['rem'] = true;
            pc['pc'].createAnswer().then(function(desc) {
                pc['pc'].setLocalDescription(desc).then(function() {
                    nextRTC.request('answerResponse', null, signal.from, desc.sdp);
                });
              });
          });
    };

    this.finalize = function(nextRTC, signal) {
        var pc = nextRTC.preparePeerConnection(nextRTC, signal.from);
        var sdpPrototype = window.RTCSessionDescription || window.mozRTCSessionDescription || window.webkitRTCSessionDescription || window.msRTCSessionDescription;
        var sdpWrapper = new sdpPrototype({
            type : 'answer',
            sdp : signal.content
        });
        
        sdpWrapper.sdp = setBandwidthSettings(sdpWrapper.sdp);
        sdpWrapper.sdp = setAudioSettings(sdpWrapper.sdp);
        
        pc['pc'].setRemoteDescription(sdpWrapper).then(function(){
            pc['rem'] = true;
        });
    };

    this.candidate = function(nextRTC, signal) {
        var pc = nextRTC.preparePeerConnection(nextRTC, signal.from);
        pc['pc'].addIceCandidate(new RTCIceCandidate(JSON.parse(signal.content.replace(new RegExp('\'', 'g'), '"'))), that.success, that.error);
    }
    
    this.init = function() {
        this.on('offerRequest', this.offerRequest);
        this.on('answerRequest', this.answerRequest);
        this.on('finalize', this.finalize);
        this.on('candidate', this.candidate);
        this.on('close', function(){ that.onCustomClose(); this.close; });
        this.on('error', this.error);
        this.on('ping', function(){}); // you can override this event
    };

    this.onIceCandidate = function(nextRTC, member, event) {
        if (event.candidate) {
            nextRTC.request('candidate', null, member, JSON.stringify(event.candidate));
        }
    }

    this.error = function(arg){
    	that.onCustomError(arg);
    	if (that.channelReady == false) {
    		that.onChannelNotReady();
    	}
    	console.log('error: ' + arg);
    }

    this.success = function(arg){
        console.log('success: ' + arg);
    }
    
    this.init();
    
//    that.onReady = function() {
//        console.log('It is highly recommended to override method NextRTC.onReady');
//    };

    // custom callback to be trigger when socket is ready
    that.onReady = config.onChannelReady;
    
    // custom callback to be triggered when it couldn't open the socket or another error 
    that.onChannelNotReady = config.onChannelNotReady;
    
    that.onCustomError = config.onCustomError;
    
    that.onCustomClose = config.onCustomClose;
};

NextRTC.prototype.on = function on(signal, operation) {
    this.signals[signal] = operation;
};

NextRTC.prototype.create = function create(from, to, convId, custom) {
    var nextRTC = this;
    navigator.mediaDevices.getUserMedia(nextRTC.mediaConfig).then(function(stream) {
	    nextRTC.localStream = stream;
	    nextRTC.call('localStream', {
	        stream : stream
	    });
	    nextRTC.request('create', from, to, convId, custom);
    }, this.error);
};

NextRTC.prototype.join = function join(from, to, convId, custom) {
    var nextRTC = this;
    navigator.mediaDevices.getUserMedia(nextRTC.mediaConfig).then(function(stream) {
        nextRTC.localStream = stream;
        nextRTC.call('localStream', {
            stream : stream
        });
        nextRTC.request('join', from, to, convId, custom);
    }, this.error);
};

NextRTC.prototype.leave = function leave(from, convId) {
    var nextRTC = this;
    nextRTC.request('left', null, null, convId, { userFrom : from });
    for(var pc in nextRTC.peerConnections){
        nextRTC.release(pc);
    }
    if(nextRTC.localStream != null){
        nextRTC.localStream.getTracks().forEach(function(track){
            track.stop();
        });
    }
};

NextRTC.prototype.release = function release(member) {
    var nextRTC = this;
    if (!nextRTC.peerConnections[member]) {
    	nextRTC.peerConnections[member].pc.close();
    }
    delete nextRTC.peerConnections[member];
};

NextRTC.prototype.close = function close() {
    var nextRTC = this;
    if(nextRTC.signaling) {
        nextRTC.signaling.close();
        console.log("Websocket closed.");
    }
    
    var hasPCs = Object.keys(nextRTC.peerConnections) > 0;
    
    for(var pc in nextRTC.peerConnections){
        nextRTC.release(pc);
    }
    
    if (hasPCs) {
    	console.log("Peer connections released.");
    }
};

//module.exports.NextRTC = NextRTC;