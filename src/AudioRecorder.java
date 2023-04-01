import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class AudioRecorder implements NativeKeyListener {
    private static final String FILENAME = "recorded_audio.wav";
    private TargetDataLine line = null;
    private ByteArrayOutputStream out = null;
    private boolean isRecording = false;

    public static void main(String[] args) {
        AudioRecorder recorder = new AudioRecorder();
        recorder.initialize();
        recorder.startKeyListener();
    }

    private void initialize() {
        AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        try {
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void startKeyListener() {
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException ex) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(ex.getMessage());
            System.exit(1);
        }

        GlobalScreen.addNativeKeyListener(this);
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if (e.getKeyCode() == NativeKeyEvent.VC_T && !isRecording) {
            System.out.println("Recording started. Press and hold 'T' to continue recording...");
            startRecording();
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        if (e.getKeyCode() == NativeKeyEvent.VC_T && isRecording) {
            System.out.println("Recording stopped.");
            stopRecording();
            saveToFile();
        }
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
    }

    private void startRecording() {
        out = new ByteArrayOutputStream();
        line.start();
        isRecording = true;

        new Thread(() -> {
            byte[] buffer = new byte[line.getBufferSize() / 5];
            int bytesRead;

            while (isRecording) {
                bytesRead = line.read(buffer, 0, buffer.length);
                out.write(buffer, 0, bytesRead);
            }
        }).start();
    }

    private void stopRecording() {
        line.stop();
        line.flush();
        isRecording = false;
    }

    private void saveToFile() {
        try {
            AudioInputStream ais = new AudioInputStream(
                    new ByteArrayInputStream(out.toByteArray()),
                    line.getFormat(),
                    out.size() / line.getFormat().getFrameSize()
            );
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(FILENAME));
            System.out.println("Recording saved to " + FILENAME);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
