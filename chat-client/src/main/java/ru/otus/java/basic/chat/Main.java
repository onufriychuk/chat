package ru.otus.java.basic.chat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Scanner;

public class Main extends JFrame implements Runnable {
    protected JTextArea outTextArea;
    protected JPanel southPanel;
    protected JTextField inTextField;
    protected JButton inTextSendButton;
    protected boolean isOn;
    Network network = new Network();

    public Main(String title, Network network) throws HeadlessException {
        super(title);
        southPanel = new JPanel();
        southPanel.setLayout(new GridLayout(2, 1, 10, 10));
        southPanel.add(inTextField = new JTextField());
        inTextField.setEditable(true);
        southPanel.add(inTextSendButton = new JButton("Send message"));
        inTextSendButton.setMnemonic(KeyEvent.VK_ENTER);
        Container cp = getContentPane();
        cp.setLayout(new BorderLayout());
        cp.add(BorderLayout.CENTER, outTextArea = new JTextArea());
        outTextArea.setEditable(false);
        cp.add(BorderLayout.SOUTH, southPanel);

        this.network = network;
        inTextSendButton.addActionListener(event ->
        {
            String text = inTextField.getText();
            try {
                network.sendMessage(text);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });


        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 600);
        setVisible(true);
        inTextField.requestFocus();
        (new Thread(this)).start();
        this.network.setCallback(args -> {
            outTextArea.append((String) args[0] + "\n");
        });
    }

    public static void main(String[] args) throws Exception {
        try (Network network = new Network()) {
            network.setCallback(args1 -> System.out.println(args1));
            network.connect(8088);
            new Main("Chat", network);
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String msg = scanner.nextLine();
                network.sendMessage(msg);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {

    }

}