package org.example;

import be.tarsos.dsp.*;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.resample.RateTransposer;

import javax.sound.sampled.*;
import java.io.*;

public class Main {

    public static void main(String[] args) throws Exception {
        float sampleRate = 44100f;
        int bufferSize = 2048;
        int overlap = bufferSize / 2;

        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);

        // Microphone setup
        TargetDataLine mic = null;
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(info);
            DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
            if (mixer.isLineSupported(dataLineInfo)) {
                mic = (TargetDataLine) mixer.getLine(dataLineInfo);
                mic.open(format, bufferSize);
                break;
            }
        }

        if (mic == null) {
            System.err.println("No suitable microphone found.");
            System.exit(1);
        }

        mic.start();

        AudioInputStream ais = new AudioInputStream(mic);
        JVMAudioInputStream tarsosStream = new JVMAudioInputStream(ais);
        AudioDispatcher dispatcher = new AudioDispatcher(tarsosStream, bufferSize, overlap);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Apply time-stretching to 1.5x speed
        dispatcher.addAudioProcessor(new RateTransposer(1.3));

        // Custom processor to convert float to 16-bit PCM
        dispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                float[] floatBuffer = audioEvent.getFloatBuffer();
                for (float sample : floatBuffer) {
                    int intSample = (int) (sample * 32767.0);
                    intSample = Math.max(-32768, Math.min(32767, intSample));
                    baos.write(intSample & 0xff);
                    baos.write((intSample >> 8) & 0xff);
                }
                return true;
            }

            @Override
            public void processingFinished() {
                // Nothing needed here
            }
        });

        // Start processing
        Thread audioThread = new Thread(dispatcher);
        audioThread.start();

        System.out.println("Recording... Press ENTER to stop.");
        System.in.read();

        dispatcher.stop();
        mic.stop();
        mic.close();

        // Save as WAV file with correct frame count
        byte[] audioData = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
        long frameCount = audioData.length / format.getFrameSize();
        AudioInputStream audioInputStream = new AudioInputStream(bais, format, frameCount);
        File outputFile = new File("output.wav");
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);

        System.out.println("Recording saved to output.wav");
    }
}
