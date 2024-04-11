import SyntaxDirectedTranslator.Translator;
import LALRTableGenerator.LALRGenerator;
import TargetCodeGenerator.CodeGenerator;
import LexicalAnalyzer.LexicAnalyzer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Interface extends JFrame {
    private JTextArea textArea;
    private JButton compileButton;

    public static void main(String[] args) {
        new Interface();
    }

    public Interface() {
        setTitle("Compiler");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        textArea = new JTextArea();
        textArea.setFont(new Font("Source Code Pro", Font.PLAIN, 24));
        compileButton = new JButton("Compile");

        compileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                compileAndGenerateResult();
            }
        });

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(textArea, BorderLayout.CENTER);
        panel.add(compileButton, BorderLayout.SOUTH);

        getContentPane().add(panel);

        setVisible(true);
    }

    private void compileAndGenerateResult() {
        String code = textArea.getText();
        String codeFilePath = "src\\Files\\Code";
        String resultFilePath = "src\\Files\\Result";
        String lexicErrorsFilePath = "src\\Files\\LexicErrors";
        String syntaxErrorsFilePath = "src\\Files\\SyntaxErrors";

        try {
            FileWriter fileWriter = new FileWriter(codeFilePath);
            fileWriter.write(code);
            fileWriter.flush();
            fileWriter.close();

            // Run method to generate result
            LALRGenerator generator = new LALRGenerator("src\\Files\\Grammar",
                    "src\\Files\\Terminals",
                    "src\\Files\\Non-terminals");
            LexicAnalyzer lexer = new LexicAnalyzer("src\\Files\\Code",
                    "src\\Files\\Tokens",
                    "src\\Files\\LexicErrors");
            Translator translator = new Translator(lexer, generator,
                    "src\\Files\\InterCode",
                    "src\\Files\\SyntaxErrors");
            CodeGenerator resultGenerator = new CodeGenerator(
                    "src\\Files\\Result", translator);
            boolean success = resultGenerator.createTargetCode();

            // Read result from file and display in new window
            String result = "";
            if (success) {
                result = readFromFile(resultFilePath);
            } else {
                String lexicErrors = readFromFile(lexicErrorsFilePath);
                String syntaxErrors = readFromFile(syntaxErrorsFilePath);
                result += lexicErrors;
                result += syntaxErrors;
            }
            displayResult(result);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private String readFromFile(String filePath) throws IOException {
        StringBuilder result = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line).append("\n");
        }
        reader.close();
        return result.toString();
    }

    private void displayResult(String result) {
        JFrame resultFrame = new JFrame("Result");
        JTextArea resultTextArea = new JTextArea(result);
        resultTextArea.setFont(new Font("Monoid", Font.PLAIN, 18));
        resultTextArea.setEditable(false);
        resultFrame.getContentPane().add(new JScrollPane(resultTextArea));
        resultFrame.setSize(400, 800);
        resultFrame.setVisible(true);
    }
}
