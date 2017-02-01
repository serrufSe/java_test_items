package je;

import org.apache.commons.cli.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;


class ConsoleLauncher {

    private Downloader downloader;

    ConsoleLauncher(Downloader downloader) {
        this.downloader = downloader;
    }

    void launch(String[] args) throws ParseException, ExecutionException, InterruptedException, IOException  {
        CommandLineParser parser = new BasicParser();

        Options options = new Options();

        options.addOption("n", true, "количество одновременно качающих потоков (1,2,3,4....)");
        options.addOption("l", true, "общее ограничение на скорость скачивания, для всех потоков, размерность - байт/секунда, можно использовать суффиксы k,m (k=1024, m=1024*1024)");
        options.addOption("f", true, "путь к файлу со списком ссылок");
        options.addOption("o", true, "имя папки, куда складывать скачанные файлы");

        CommandLine commandLine = parser.parse(options, args);

        downloader.start(Integer.parseInt(commandLine.getOptionValue("n")), lToLimit(commandLine.getOptionValue("l")),
                commandLine.getOptionValue("f"), commandLine.getOptionValue("o"));
    }

    private static int lToLimit(String lArgument) {
        if (lArgument.endsWith("m")) {
            return 1024*1024*ConsoleLauncher.getNumPart(lArgument);
        } else if (lArgument.endsWith("k")) {
            return 1024*ConsoleLauncher.getNumPart(lArgument);
        }
        return Integer.parseInt(lArgument);
    }

    private static int getNumPart(String expression) {
        return Integer.parseInt(expression.substring(0, expression.length() - 1));
    }

}

class Downloader {

    void start(int threadCount, int limit, String sourceFolder, String targetFolder) throws ExecutionException, InterruptedException {
        ForkJoinPool forkJoinPool = new ForkJoinPool(threadCount);

        forkJoinPool.submit(() -> {
                    try {
                        Files.lines(Paths.get(sourceFolder)).forEach(System.out::println);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        ).get();
    }

}

public class ConsoleDownloader {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        try {
            new ConsoleLauncher(new Downloader()).launch(args);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
