package org.fabri1983.signaling;

import java.io.PrintStream;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;
import org.springframework.core.env.Environment;

public class SignalingServerBanner implements Banner {
	
	private final String[] BANNER = {
			"  ______  _                       _   _                   ______",
			" / _____)(_)                     | | (_)                 / _____)",
			"( (____   _   ____  ____   _____ | |  _  ____    ____   ( (____   _____   ____  _   _  _____   ____", 
			" \\____ \\ | | / _  ||  _ \\ (____ || | | ||  _ \\  / _  |   \\____ \\ | ___ | / ___)| | | || ___ | / ___)",
			" _____) )| |( (_| || | | |/ ___ || | | || | | |( (_| |   _____) )| ____|| |     \\ V / | ____|| |",
			"(______/ |_| \\___ ||_| |_|\\_____| \\_)|_||_| |_| \\___ |  (______/ |_____)|_|      \\_/  |_____)|_|",  
			"            (_____|                            (_____|",
			};

	private final String SPRING_BOOT = " :: Spring Boot :: ";

	private final int STRAP_LINE_SIZE = 42;

	@Override
	public void printBanner(Environment environment, Class<?> sourceClass,
			PrintStream printStream) {
		for (String line : BANNER) {
			printStream.println(line);
		}
		String version = SpringBootVersion.getVersion();
		version = (version == null ? "" : " (v" + version + ")");
		String padding = "";
		while (padding.length() < STRAP_LINE_SIZE
				- (version.length() + SPRING_BOOT.length())) {
			padding += " ";
		}

		printStream.println(AnsiOutput.toString(AnsiColor.GREEN, SPRING_BOOT,
				AnsiColor.DEFAULT, padding, AnsiStyle.FAINT, version));
		printStream.println();
	}
	
}
