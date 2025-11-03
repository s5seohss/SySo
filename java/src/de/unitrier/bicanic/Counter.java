package de.unitrier.bicanic;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Counter {

	public static void main(String[] args) throws IOException {
		assert (args.length == 3);
		int threadCount = Integer.parseInt(args[2]);
		long startTime = System.currentTimeMillis();
		long total = countLinesInAllFilesParallel(args[0], args[1], threadCount);
		long endTime = System.currentTimeMillis();
		System.out.println("Anzahl der Zeilen: " + total);
		System.out.println("Anzahl der Threads: " + threadCount);
		System.out.println("Benötigte Zeit: " + (endTime - startTime) + " ms");
	}

	public static long countLines(String fileName) throws IOException {
		try (Stream<String> lines = Files.lines(Paths.get(fileName))) {
			return lines.count();
		}
	}

	public static long countLinesInAllFilesParallel(String folderPath, String regex, int threadCount) throws IOException {
		Pattern pattern = Pattern.compile(regex);

		List<Path> files;

		try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
			files = paths.filter(Files::isRegularFile)
					.filter(path -> pattern.matcher(path.getFileName().toString()).matches())
					.collect(Collectors.toList());
		}
		List<Thread> threads = new ArrayList<>();
		List<Long> results = new ArrayList<>();

		int filesPerThread = (int) Math.ceil((double) files.size() / threadCount);
		System.out.println("Anzahl der Dateien: " + filesPerThread);

		for (int i = 0; i < threadCount; i++) {
			int start = i * filesPerThread;
			int end = Math.min(start + filesPerThread, files.size());

			if (start >= end) break;

			List<Path> subset = files.subList(start, end);

			Thread t = new Thread(() -> {
				long sum = 0;
				for (Path path : subset) {
					try{
						sum += countLines(path.toString());
					}catch (IOException e){
						throw new UncheckedIOException(e);
					}
				}
				synchronized (results) {
					results.add(sum);
				}

			});
			threads.add(t);
			t.start();
		}
		for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
		return results.stream().mapToLong(Long::longValue).sum();
	}

}

