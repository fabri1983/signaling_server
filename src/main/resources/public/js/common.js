/**
 * Parse URL query parameters and return them in a map.
 */
function getUrlQueryParams() {
	var url = document.URL; //window.location.href;
	var vars = {};
	var splitArray = url.split("?");
	
	if (splitArray.length > 1) {
		var hashes = splitArray[1];
		var hash = hashes.split('&');
	
		for (var i = 0; i < hash.length; i++) {
			var params = hash[i].split("=");
			if (params.length > 1) {
				vars[params[0]] = params[1]; // both params are String type
			}
		}
	}
	return vars;
}

var heartBeatInitialized = false;
var pingReceived = false;
var intervalHeartbeat;

function intervalHeartbeatFunc(ifNotReceived) {
	if (!pingReceived) {
		stopHeartbeat();
		ifNotReceived();
	}
	pingReceived = false;
}

function startIntervalHeartbeat(everyNmillis, ifNotReceived) {
	if (heartBeatInitialized == true)
		return;
	heartBeatInitialized = true;
	// start the interval function for the heartbeat (for some reason it works with seconds instead of millis)
	intervalHeartbeat = setInterval(intervalHeartbeatFunc, (everyNmillis + 100)/1000, ifNotReceived);
}

function stopHeartbeat() {
	clearInterval(intervalHeartbeat);
	heartBeatInitialized = false;
	pingReceived = false;
}

function heartbeatReceived() {
	pingReceived = true;
}

/**
 * Username and Password generation for secured turn server usage with --use-auth-secret.
 * 
 *   temporary-username = "timestamp" + ":" + "username"
 *   temporary-password = base64_encode(hmac-sha1(shared-secret, temporary-username))
 *   
 * Requires sjcl and sha1 javascript files.
 *  
 * @param uname
 * @param sharedSecret
 * @returns
 */
function getTURNCredentials(uname, sharedSecret, expirationInSeconds){

	var unixTimeStamp = parseInt(Date.now()/1000) + expirationInSeconds; // javascript does not support long
	var usernameCombo = [unixTimeStamp, uname].join(':');
	//var sharedSecretUtf8 = encodeURIComponent(sharedSecret); // I think is not neeed if you declared <meta> with charset=UTF-8 in your html 
	var hmacObj = new sjcl.misc.hmac( sjcl.codec.utf8String.toBits(sharedSecret), sjcl.hash.sha1 );
	hmacObj.update( sjcl.codec.utf8String.toBits(usernameCombo) );
	var passwordInBits = hmacObj.digest();
	var passwordBase64 = sjcl.codec.base64.fromBits(passwordInBits);
	return {
		user: usernameCombo,
		pass: passwordBase64
	};
}

function isTurnServerUsingAuthSecret() {
	return true;
}

function loadAllServers() {

	var dblClickSelect = function(event){ selectServer(event); };
	var dblClickCopyToBox = function(){ copyServerToBox(); };

	// Default credentials to use when not using flag --use-auth-secret.
	// Created with turnadmin tool.
	var creds = {
		user: "${turn.no.auth.user}",
		pass: "${turn.no.auth.pass}"
	};
	
	// Credentials used to access turn server when using flag --use-auth-secret
	// Shared Secret is specified with turnadmin tool.
	if (isTurnServerUsingAuthSecret()) {
		creds = getTURNCredentials("${turn.auth.secret.user}", "${turn.auth.secret.pass}", 3600*30); // expires in 1800secs = 30mins
	}
	
	addOptionToSelect('servers', 
			'{ "urls" : [ "stun:stun.l.google.com:19302" ] }', 
			dblClickSelect);
	
	// google stun server for testing purposes
	addOptionToSelect('serversAll',
			'{ "urls" : [ "stun:stun.l.google.com:19302" ] } ', 
			dblClickCopyToBox);
	
	// twilio server
	addOptionToSelect('serversAll',
			'{ "urls" : [ "stun:global.stun.twilio.com:3478?transport=tcp" ] } ', 
			dblClickCopyToBox);
	addOptionToSelect('serversAll',
			'{ "urls" : [ "stun:global.stun.twilio.com:3478?transport=udp" ] } ', 
			dblClickCopyToBox);
	
	// public turn server for testing purposes
	addOptionToSelect('serversAll', 
			'{ "urls": [ "turn:numb.viagenie.ca" ], "username": "louis@mozilla.com", "credential": "webrtcdemo" }',
			dblClickCopyToBox);
}

function addOptionToSelect(selectId, jsonString, dblCLick) {
	// parse to Json format
	var iceServerJson = JSON.parse(jsonString);
	
	// store the ICE server as a stringified JSON object in option.value
	var option = document.createElement('option');
	option.value = JSON.stringify(iceServerJson);
	
	// set the text in the option.text
	option.text = iceServerJson.urls[0] + ' ';
	var username = iceServerJson.username;
	var password = iceServerJson.credential;
	if (username || password) {
		option.text += (' [' + username + ':' + password + ']');
	}
	
	// add double click event
	option.ondblclick = dblCLick;
	
	// add the option object in the select. Don't use JQuery here
	document.getElementById(selectId).add(option);
}

function checkTURNServerFunc(selectId, buttonId) {
	
	var iceServerSelected = $('#'+selectId+' option:selected');
	
	// only TURN servers can be verified
	var scheme = iceServerSelected.text().trim().split(':')[0];
	if (!scheme || !(scheme == 'turn' || scheme == 'turns')) {
		$('#log').append("<li>Only turn/turns server can be verified!</li>");
		return;
	}

	$('#'+buttonId).prop('disabled', true).css('opacity', 0.5);
	
	var elemId = 'checkTurnServerId_' + Math.floor((Math.random() * 10000) + 1);
	var logLine = "Is <b>" + iceServerSelected.text().trim() + "</b> active? .";
	$('#log').append("<li id='"+ elemId + "'>" + logLine + "</li>");
	
	// read the values from the option element
	var iceServer = JSON.parse(iceServerSelected.val());
	
	// run a function every 250 ms to update the logging line
	var intervalLogger = setInterval(function(){
		logLine +=  '.';
		$('#'+elemId).html(logLine);
	}, 250);
	
	// check TURN server connectivity with a timeout of 4 secs
	checkTURNServerPromise(iceServer, 4000)
	.then( function(bool){
		$('#'+buttonId).prop('disabled', false).css('opacity', 1.0);
		clearInterval(intervalLogger);
		$('#'+elemId).html(logLine + (bool? ' <b>ACTIVE</b>':' <b>INACTIVE</b> (4 secs timeout)'));
	})
	.catch( function(){
		$('#'+buttonId).prop('disabled', false).css('opacity', 1.0);
		clearInterval(intervalLogger);
		console.error.bind(console);
	});
}

function checkTURNServerPromise(turnConfig, timeout) { 

  return new Promise(function(resolve, reject) {

    setTimeout(function(){
        if(promiseResolved) return;
        resolve(false);
        promiseResolved = true;
    }, timeout || 5000);

    var promiseResolved = false;
    // compatibility for firefox and chrome
    var myPeerConnection = window.RTCPeerConnection || window.mozRTCPeerConnection || window.webkitRTCPeerConnection || window.msRTCPeerConnection;
    var pc = new myPeerConnection({iceServers:[turnConfig]});
    var noop = function(){};
    
    pc.createDataChannel(""); // create a bogus data channel
    
    pc.createOffer(function(sdp){
      if(sdp.sdp.indexOf('typ relay') > -1){ // sometimes sdp contains the ice candidates...
        promiseResolved = true;
        resolve(true);
      }
      pc.setLocalDescription(sdp, noop, noop);
    }, noop); // create offer and set local description
    
    pc.onicecandidate = function(ice){ // listen for candidate events
      if(promiseResolved || !ice || !ice.candidate || !ice.candidate.candidate || !(ice.candidate.candidate.indexOf('typ relay')>-1)) return;
      promiseResolved = true;
      resolve(true);
    };
  });   
}

function collectIceServers() {
	// read the options from the select box. Do not use JQuery
	var iceServers = [];
	var servers = document.getElementById('servers');
	for (var i = 0; i < servers.length; ++i) {
		iceServers.push(JSON.parse(servers[i].value));
	}
	return iceServers;
}

function selectServer(event) {
	var option = event.target;
	var iceServerJson = JSON.parse(option.value);
	$('#serverUrl').val(iceServerJson.urls[0]);
	$('#serverUsername').val(iceServerJson.username || '');
	$('#serverPassword').val(iceServerJson.credential || '');
}

function addServer() {
	if (!$('#serverUrl').val() || $('#serverUrl').val() == '')
		return;
	
	var scheme = $('#serverUrl').val().split(':')[0];
	if (!scheme || scheme.length == 0 || (scheme !== 'stun' && scheme !== 'turn' && scheme !== 'turns')) {
		alert('URI scheme ' + scheme + ' is not valid');
		return;
	}
	
	// store the ICE server as a stringified JSON object in option.value
	var option = document.createElement('option');
	var iceServer = {
		urls: [$('#serverUrl').val()],
		username: $('#serverUsername').val(),
		credential: $('#serverPassword').val()
	};
	option.value = JSON.stringify(iceServer);
	
	// set the text in the option.text
	option.text = $('#serverUrl').val() + ' ';
	var username = $('#serverUsername').val();
	var password = $('#serverPassword').val();
	if (username || password) {
		option.text += (' [' + username + ':' + password + ']');
	}
	
	// add double click event
	option.ondblclick = function(event){ selectServer(event); };
	
	// add the option object in the select. Don't use JQuery here
	document.getElementById('servers').add(option);
	
	$('#serverUrl').val('');
	$('#serverUsername').val('');
	$('#serverPassword').val('');
}

function removeServer() {
	// don't use JQuery here
	var servers = document.getElementById('servers');
	for (var i = servers.options.length - 1; i >= 0; --i) {
		if (servers.options[i].selected) {
			servers.remove(i);
		}
	}
}

function copyServerToBox() {
	
	// selected option to be copied to other select element
	var iceServerSelected = $('#serversAll option:selected');
	
	// copy the selected option 
	var option = document.createElement('option');
	option.value = iceServerSelected.val();
	option.text = iceServerSelected.text();
	
	// add double click event
	option.ondblclick = function(event){ selectServer(event); };
	
	// add the option object in the select. Don't use JQuery here
	document.getElementById('servers').add(option);
}

function getIpFromHostname(hostname, callback) {
	$.ajax({
		url: 'https://api.exana.io/dns/' + hostname + '/a',
	    cache: false,
	    method: 'GET',
	    dataType: 'jsonp',
	    error: function(xhr, error) {
	    	console.error("Problem! Couldn't retrieve ip from hostname " + hostname + ". See Response in Network tab on Developer Tools console.");
	    },
	    success: function(data) {
	    	// data.answer is a list
	    	var ips = [];
	    	data.answer.forEach(function(elem){ ips.push(elem.rdata); });
	    	console.info(ips);
	    	callback(ips);
	    }
	});
}

function setBandwidthSettings (sdp) {
	//############################################################################################
	// According to webrtc-experiment (https://www.webrtc-experiment.com/webrtcpedia/) the minimum bandwidth for 
	// Opus is 6kbit/s and for VP8 100kbits/s. 
	// So in total that makes 106kbit/s but when you account for the overhead of the webrtc protocol stack and constantly 
	// varying network conditions I would guess that 200kbit/s is the minimum if one wants stable video and audio.
	//
	// Audio: real time audio typically has a bitrate of 40-200kbit/s
	// Video: video requires at least a bitrate of 200 kbit/s (500kbit/s if you want to see people's faces).
	// 
	// See graphs at url: chrome://webrtc-internals
	//############################################################################################
	
	// NOTE: bandwidth is not the same than bitrate
	var bandwidth = {
		screen : 300,  // 300kbits minimum
		audio : 50,    // 50kbits minimum
		video : 100    // 100kbits (both min-max)
	};
	var isScreenSharing = false;
	sdp = BandwidthHandler.setApplicationSpecificBandwidth(sdp, bandwidth, isScreenSharing);
	sdp = BandwidthHandler.setVideoBitrates(sdp, {
		min : bandwidth.video,
		max : bandwidth.video
	});
	return sdp;
}

function setAudioSettings (sdp) {
	sdp = BandwidthHandler.setOpusAttributes(sdp, {
		'stereo' : 0, // disable stereo (force mono audio)
		'sprop-stereo' : 1,
		'maxaveragebitrate' : 200 * 1024 * 8, // 200 kbits (bitrate)
		'maxplaybackrate' : 200 * 1024 * 8, // 200 kbits (bitrate?)
		'cbr' : 0, // disable cbr
		'useinbandfec' : 1, // use inband fec
		'usedtx' : 1, // use dtx
		'maxptime' : 3
	});
	return sdp;
}

function guid() {
	return s4() + s4() + '' + s4() + '' + s4() + '' + s4() + '' + s4() + s4() + s4();
}

function s4() {
	return Math.floor((1 + Math.random()) * 0x10000)
		.toString(16)
		.substring(1);
}
