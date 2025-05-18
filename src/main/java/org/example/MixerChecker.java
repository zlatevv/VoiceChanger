package org.example;

import javax.sound.sampled.*;

public class MixerChecker {

    public static void main(String[] args) {
        float sampleRate = 44100;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;

        AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);

        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();

        System.out.println("Looking for mixers that support format: " + format);

        for (Mixer.Info mixerInfo : mixerInfos) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            Line.Info lineInfo = new DataLine.Info(TargetDataLine.class, format);

            boolean supported = mixer.isLineSupported(lineInfo);

            System.out.println("Mixer: " + mixerInfo.getName() + " - Supported: " + supported);
        }
    }
}


