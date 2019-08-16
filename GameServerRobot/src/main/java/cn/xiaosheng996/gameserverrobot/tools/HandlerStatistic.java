package cn.xiaosheng996.gameserverrobot.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.protobuf.Message;

public final class HandlerStatistic {
	private static final Log log = LogFactory.getLog(HandlerStatistic.class);

	private static boolean opened = false;

	public static class Stats {
		private Class<? extends Message> clazz;
		private long count = 0;
		private long total = 0;
		private long min = Long.MAX_VALUE;
		private long max = Long.MIN_VALUE;
		private long minSize = 0;
		private long maxSize = 0;
		private long totalSize = 0;

		public Stats(Class<? extends Message> clazz) {
			this.clazz = clazz;
		}

		private synchronized void stats(long time, int size) {
			count++;
			total += time;
			min = Math.min(min, time);
			max = Math.max(max, time);
			totalSize += size;
			minSize = Math.min(minSize, size);
			maxSize = Math.max(maxSize, size);
		}

		@Override
		public synchronized String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("proto:").append(clazz.getSimpleName()).append(",")
					.append("called:").append(count).append(",")
					.append("avg:").append(count==0?0:total/count).append("ms,")
					.append("min:").append(min==Long.MAX_VALUE?0:min).append("ms,")
					.append("max:").append(max==Long.MIN_VALUE?0:max).append("ms,")
					.append("avgSize:").append(count==0?0:totalSize/count).append("bytes,")
					.append("minSize:").append(minSize==Long.MAX_VALUE?0:minSize).append("bytes,")
					.append("maxSize:").append(maxSize==Long.MIN_VALUE?0:maxSize).append("bytes.");
			return sb.toString();
		}
	}

	private static final ConcurrentMap<Class<? extends Message>, Stats> STATS = new ConcurrentHashMap<>();

	public static void stats(Class<? extends Message> clazz, long time, int size) {
		if (!opened)
			return;
		Stats stats = STATS.get(clazz);
		if (stats == null) {
			stats = new Stats(clazz);
			Stats old = STATS.putIfAbsent(clazz, stats);
			if (old != null)
				stats = old;
		}
		stats.stats(time, size);
	}

	public static void openStats() {
		opened = true;
	}

	public static void dump(File file) {
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(file);
			fileWriter.append("==================statistic of handler begin(")
					.append(new Date().toString())
					.append(")==================\n");
			for (Map.Entry<Class<? extends Message>, Stats> entry : STATS.entrySet()) {
				Stats stats = entry.getValue();
				fileWriter.append(stats.toString()).append("\n");
			}
			fileWriter.append("==================statistic of handler end(")
					.append(new Date().toString())
					.append(")==================\n");
			fileWriter.flush();
		} catch (IOException e) {
			log.error("write file failed", e);
		} finally {
			if (fileWriter != null) {
				try {
					fileWriter.close();
				} catch (IOException e) {
					log.error("close file writer failed", e);
				}
			}
		}
	}
}
