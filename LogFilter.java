import java.io.*;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
 * ログファイルをフィルタリングして、検索文字列に一致する行を抽出するクラス。
 */
public class LogFilter {

    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        while (true) {
            // 対象のログファイルパス
            String logFilePath = promptForLogFilePath();

            // 検索文字列を入力し続ける
            while (!promptAndProcessSearchString(logFilePath)) {
                // このループ内では関数の呼び出しが全ての作業を行う
            }
        }
    }

    /**
     * ユーザーにログファイルのパスを入力してもらいファイル検証する。
     *
     * @return 検証済みのログファイルのパス。
     */
    private static String promptForLogFilePath() {
        String logFilePath;
        File logFile;

        while (true) {
            System.out.println("ログファイル名を入力してください：");
            logFilePath = scanner.nextLine();
            logFile = new File(logFilePath);
            // 検証結果、問題なければパスを返却
            if (logFile.exists() && !logFile.isDirectory() && logFilePath.endsWith(".log") && isValidUTF8(logFile)) {
                return logFilePath;
            }
            printInvalidLogFilePathMessage(logFilePath, logFile);
        }
    }

    /**
     * ログファイルの検証チェックに基づいてエラーメッセージを出力する。
     *
     * @param logFilePath 入力されたログファイルのパス。
     * @param logFile     入力されたログファイルのFileオブジェクト。
     */
    private static void printInvalidLogFilePathMessage(String logFilePath, File logFile) {
        if (!logFile.exists()) {
            System.out.println("存在しないファイルまたはディレクトリです。");
        } else if (!logFilePath.endsWith(".log")) {
            System.out.println("指定されたファイルは.log拡張子ではありません。");
        } else if (!isValidUTF8(logFile)) {
            System.out.println("指定されたファイルはUTF-8でエンコードされていません。");
        } else {
            System.out.println("指定されたファイルは不正です。");
        }
    }

    /**
     * 与えられたファイルがUTF-8でエンコードされているかどうかをチェックする。
     *
     * @param file チェックするファイル。
     * @return UTF-8でエンコードされていればtrue、それ以外はfalse。
     */
    private static boolean isValidUTF8(File file) {
        try {
            Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            return true;
        } catch (MalformedInputException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * ユーザーに検索文字列を入力してもらい、ログファイルを検証し出力ファイルにする。
     *
     * @param logFilePath 処理するログファイルのパス。
     * @return 検索文字列が一致する場合はtrue、それ以外はfalse。
     */
    private static boolean promptAndProcessSearchString(String logFilePath) {
        System.out.println("検索文字列を入力してください：");
        String searchString = scanner.nextLine();
        processLogFile(logFilePath, searchString);
        return promptForNextAction();

    }

    /**
     * 次のアクションをユーザーに求め、入力を処理する。
     *
     * @return ログファイルのパスを再入力する場合はtrue、それ以外はfalse。
     */
    private static boolean promptForNextAction() {
        System.out.println("次のアクションを選んでください：");
        System.out.println("1. 新しい検索文字列を入力");
        System.out.println("2. ログファイル名を再入力");
        System.out.println("3. 終了する");

        while (true) {
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    return false; // 検索文字列を再入力
                case "2":
                    return true; // ログファイルのパスを再入力
                case "3":
                    scanner.close();
                    System.exit(0);
                    break; // 実際には必要ないが、明確さのために追加
                default:
                    System.out.println("無効な選択です。1、2、3のいずれかを入力してください。");
            }
        }
    }

    /**
     * 文字列がファイル名として無効な文字を含んでいるかどうかをチェックします。
     *
     * @param input チェックする文字列。
     * @return 無効な文字を含んでいればtrue、それ以外はfalse。
     */
    private static boolean containsInvalidCharacters(String input) {
        return input.chars().anyMatch(ch -> "<>:\"/\\|?*".indexOf(ch) >= 0);
    }

    /**
     * ログファイルから検索文字列に一致する行を抽出して新しいファイルに書き出す。
     *
     * @param logFilePath  処理するログファイルのパス。
     * @param searchString 検索文字列。
     * @return マッチする行があればtrue、それ以外はfalse。
     */
    private static void processLogFile(String logFilePath, String searchString) {
        Path path = Paths.get(logFilePath);
        String logFileName = path.getFileName().toString();
        Path outputPath = buildOutputPath(logFileName, searchString);

        boolean isMatched = false; // マッチする行があるかどうかをチェック
        try {
            Files.createDirectories(outputPath.getParent());
            byte[] fileBytes = Files.readAllBytes(path);
            String content = new String(fileBytes, StandardCharsets.UTF_8);
            String[] lines = content.split("\n");
            try (BufferedWriter bw = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
                bw.write("Search String: " + searchString);
                bw.newLine();
                for (String line : lines) {
                    if (line.contains(searchString)) {
                        bw.write(line);
                        isMatched = true;
                    }
                }
                if (!isMatched) {
                    System.out.println("指定された検索文字列はヒットしませんでした。");
                } else {
                    System.out.println("指定された検索文字列がヒットしました。");
                }
            }
        } catch (IOException e) {
            System.err.println("ファイルの処理中にエラーが発生しました: " + e.getMessage());
            System.exit(1);
        }
        System.out.println("ファイル出力が終了しました。");
    }

    /**
     * 出力ファイルのパスを生成する。
     *
     * @param logFileName  処理するログファイルの名前。
     * @param searchString 検索文字列。
     * @return 出力ファイルのパス。
     */
    private static Path buildOutputPath(String logFileName, String searchString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        // 入力文字列がファイル名にファイル名に適切な場合とそうでない場合でディレクトリ構造を変える
        if (containsInvalidCharacters(searchString)) {
            return Paths.get("output", logFileName, timestamp + ".txt");
        } else {
            return Paths.get("output", logFileName, searchString, timestamp + ".txt");
        }
    }
}
