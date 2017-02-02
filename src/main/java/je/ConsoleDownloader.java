package je;

import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.cli.*;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


class Report {

    private long start;
    private int bytes;

    Report() {
        start = System.currentTimeMillis();
    }

    long totalTime() {
        return System.currentTimeMillis() - start;
    }

    void addBytes(int bytes) {
        this.bytes += bytes;
    }

    int totalBytes() { return bytes;}
}


class ConsoleLauncher {

    private Downloader downloader;

    ConsoleLauncher(Downloader downloader) {
        this.downloader = downloader;
    }

    Report launch(String[] args) throws ParseException, ExecutionException, InterruptedException, IOException {
        CommandLineParser parser = new BasicParser();

        Options options = new Options();

        options.addOption("n", true, "количество одновременно качающих потоков (1,2,3,4....)");
        options.addOption("l", true, "общее ограничение на скорость скачивания, для всех потоков, размерность - байт/секунда, можно использовать суффиксы k,m (k=1024, m=1024*1024)");
        options.addOption("f", true, "путь к файлу со списком ссылок");
        options.addOption("o", true, "имя папки, куда складывать скачанные файлы");

        CommandLine commandLine = parser.parse(options, args);

        return downloader.start(Integer.parseInt(commandLine.getOptionValue("n")), lToLimit(commandLine.getOptionValue("l")),
                commandLine.getOptionValue("f"), commandLine.getOptionValue("o"));
    }

    private static int lToLimit(String lArgument) {
        if (lArgument.endsWith("m")) {
            return 1024 * 1024 * ConsoleLauncher.getNumPart(lArgument);
        } else if (lArgument.endsWith("k")) {
            return 1024 * ConsoleLauncher.getNumPart(lArgument);
        }
        return Integer.parseInt(lArgument);
    }

    private static int getNumPart(String expression) {
        return Integer.parseInt(expression.substring(0, expression.length() - 1));
    }
}


class Downloader {

    private RateLimiter rateLimiter;
    private Report report;

    Downloader() {
        report = new Report();
    }

    Report start(int threadCount, int limit, String sourceFolder, String targetFolder) throws ExecutionException,
            InterruptedException, IOException {
        rateLimiter = RateLimiter.create(limit);
        ExecutorService service = Executors.newFixedThreadPool(threadCount);

        Files.lines(Paths.get(sourceFolder))
                .map((line) -> line.split(" "))
                .forEach((item) -> {
                    service.submit(() -> {
                        try {
                            download(item[0], Paths.get(targetFolder, item[1]).toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                });
        return report;
    }

    private void download(String source_url, String target_name) throws IOException {
        URL url = new URL(source_url);
        BufferedInputStream bis = new BufferedInputStream(url.openStream());
        FileOutputStream fis = new FileOutputStream(target_name);
        byte[] buffer = new byte[1024];
        int count;
        while ((count = bis.read(buffer)) > 0) {
            report.addBytes(count);
            rateLimiter.acquire(count);
            fis.write(buffer, 0, count);
        }
    }

}

public class ConsoleDownloader {

    public static void main(String[] args) {
        try {
            Report report = new ConsoleLauncher(new Downloader()).launch(args);
            System.out.println("Total bytes " + report.totalBytes());
            System.out.println("Total time " + report.totalTime());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
