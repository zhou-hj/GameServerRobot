package cn.xiaosheng996.gameserverrobot.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class Probability {
    private static char[] numbersAndLetters = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private Probability() {
    }

    public static Random getRandom() {
        return ThreadLocalRandom.current();
    }

    /**
     * 获取随机数
     * @return Integer, null: when max < min
     */
    public static final Integer rand(int min, int max) {
        int tmp = max - min;
        if (tmp < 0) {
            return null;
        } else if (tmp == 0) {
            return min;
        } else {
            return getRandom().nextInt(tmp + 1) + min;
        }
    }

    /**
     * 获取随机数
     *
     * @return Integer, null: when max < min
     */
    public static final Long rand(long min, long max) {
        int tmp = Math.abs((int) (max - min));
        if (tmp < 0) {
            return null;
        } else if (tmp == 0) {
            return min;
        } else {
            return getRandom().nextInt(tmp + 1) + min;
        }
    }

    /**
     * 获取随机数(double)
     *
     * @param min 最小值
     * @param max 最大值
     * @param round 保留多少位小数（不能太大）
     * @return Double, null: when max < min
     */
    public static final Double rand(double min, double max, int round) {
        double dRound = Math.pow(10, round);
        int iMin = (int) (min * dRound);
        int iMax = (int) (max * dRound);
        Integer r = rand(iMin, iMax);
        if (r == null) {
            return null;
        }
        return r / dRound;
    }

    /**
     * 获取概率事件，几率最多支持3位小数
     *
     * @param map 参数举例: Map(1=>20.1, 2=>29.9, 3=>50), 则20.1%几率返回1, 29.9%返回2, 50%返回3
     * @return 返回键值
     */
    public static final <T> T getRand(Map<T, Double> map) {
        int multiple = 1000; // 放大位数

        // 求和
        int sum = 0;
        Iterator<Entry<T, Double>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<T, Double> entry = iter.next();
            Double v = entry.getValue();
            sum += v * multiple;
        }

        if (sum <= 0) {
            return null;
        }

        // 产生0-sum的整数随机
        int luckNum = getRandom().nextInt(sum) + 1;
        int tmp = 0;
        iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<T, Double> entry = iter.next();
            Double v = entry.getValue();
            tmp += v * multiple;
            if (luckNum <= tmp) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 获取概率事件，几率最多支持3位小数
     *
     * @param map 参数举例: HashMap(1=>20.1, 2=>29.9, 3=>50), 则20.1%几率返回1, 29.9%返回2, 50%返回3
     * @return 返回键值
     */
    public static final <T> T getRand(Set<T> map) {
        int multiple = 1000; // 放大位数
        // 求和
        int sum = map.size() * multiple;

        if (sum <= 0) {
            return null;
        }

        // 产生0-sum的整数随机
        int luckNum = getRandom().nextInt(sum) + 1;
        int tmp = 0;
        for (T one : map) {
            tmp += multiple;
            if (luckNum <= tmp) {
                return one;
            }
        }
        return null;
    }

    /**
     * 从集合中随机一个元素
     *
     * @date: 2016年4月12日 下午1:18:08
     */
    public static final <T> T getRand(Collection<T> map) {
        int multiple = 1000; // 放大位数
        // 求和
        int sum = map.size() * multiple;

        if (sum <= 0) {
            return null;
        }

        // 产生0-sum的整数随机
        int luckNum = getRandom().nextInt(sum) + 1;
        int tmp = 0;
        for (T one : map) {
            tmp += multiple;
            if (luckNum <= tmp) {
                return one;
            }
        }
        return null;
    }

    /**
     * 获取概率事件
     *
     * @param wildsArr 字符数组
     * @param num 返回结果个数
     * @return 选中结果（会有重复的）
     */
    public static final int[] getRandByStrArray(int[] wildsArr, int num) {
        int[] ret = new int[num];
        int len = wildsArr.length;
        for (int i = 0; i < num; i++) {
            int luckNum = getRandom().nextInt(len);
            System.arraycopy(wildsArr, luckNum, ret, i, 1);
        }
        return ret;
    }

    /**
     * 由概率随机是否触发
     *
     * @param chance 0.00 - 100.00
     * @return boolean
     */
    public static final boolean isLuck(double chance) {
        // chance = 50.0;// 测试：30%成功
        int chanceInt = (int) chance * 100;
        return Probability.rand(1, 10000) <= chanceInt;
    }

    /**
     * 获取两个数之间的随机数，得出的值符合正态分布
     *
     * @param min 最小值
     * @param max 最大值
     * @param factor 调整曲线参数
     */
    public static final int gaussianRand(int min, int max, int factor) {
        if (min > max) {
            return 0;
        }
        int middle = (int) Math.ceil((min + max) / 2.0);
        int in = max - middle;
        factor = factor < 0 ? 0 : factor;
        LinkedHashMap<Integer, Double> map = new LinkedHashMap<Integer, Double>();
        for (int i = min; i <= max; i++) {
            if (i == middle) {
                map.put(i, (double) in);
            } else {
                double tmp = Math.abs(in - Math.abs(middle - i));
                tmp = tmp <= 0 ? 1 : tmp;
                tmp = factor > 0 ? tmp * (1 + factor / 100.0) : tmp;
                tmp = tmp > in ? in : tmp;
                map.put(i, tmp);
            }
        }
        return Probability.getRand(map);
    }

    /**
     * 获取两个数之间的随机数，得出的值符合正态分布
     *
     * @param min 最小值
     * @param max 最大值
     */
    public static final int gaussianRand(int min, int max) {
        return gaussianRand(min, max, 0);
    }

    /**
     * 在一个范围内随机一定量的的数值
     *
     */
    public static List<Integer> randomList(int start, int end, int num) {
        List<Integer> reslut = new ArrayList<>();
        if (start >= end) {
            return reslut;
        }
        // 包含头尾，返回所有可能，打乱顺序
        if ((end - start + 1) <= num) {
            for (int i = start; i <= end; i++) {
                reslut.add(i);
            }
            Collections.shuffle(reslut);
            return reslut;
        }
        // 列举所有数值
        List<Integer> temp = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            temp.add(i);
        }
        // 取出一个删除一个，然后继续再剩余的列表随机
        for (int i = 0; i < num; i++) {
            int index = rand(0, temp.size() - 1);
            Integer value = temp.get(index);
            if (!reslut.contains(value)) {
                temp.remove(index);
                reslut.add(value);
            }
        }
        return reslut;
    }

    /**
     * 在一组数据里面随机几个值
     *
     * @param array 随机的源数据
     * @param num 获取数据个数
     */
    public static <T> List<T> getRandByArray(T[] array, int num) {
        List<T> reslut = new ArrayList<>();
        int maxIndex = array.length;
        List<Integer> randomIndex = randomList(0, maxIndex - 1, num);
        for (Integer index : randomIndex) {
            if (index == null || index < 0 || index > maxIndex - 1) {
                continue;
            }
            if (reslut.size() >= num) {
                break;
            }
            reslut.add(array[index]);
        }
        return reslut;
    }

    /**
     * 在一组数据里面随机几个值
     */
    public static <T> List<T> getRandByArray(List<T> list, int num) {
        List<T> reslut = new ArrayList<>();
        int maxIndex = list.size();
        List<Integer> randomIndex = randomList(0, maxIndex - 1, num);
        for (Integer index : randomIndex) {
            if (index == null || index < 0 || index > maxIndex - 1) {
                continue;
            }
            if (reslut.size() >= num) {
                break;
            }
            reslut.add(list.get(index));
        }
        return reslut;
    }

    /**
     * 从[0-9] 和 [A-Z] 中随机定长的字符串
     * @param length 生成字符串长度
     */
    public static String randomString(int length) {
        if (length <= 0) {
            return null;
        }
        char[] randBuffer = new char[length];
        for (int i = 0; i < randBuffer.length; i++) {
            randBuffer[i] = numbersAndLetters[getRandom().nextInt(numbersAndLetters.length - 1)];
        }
        return new String(randBuffer);
    }

    /**
     * 通用事件概率
     */
    public static class RateObject implements RandomObject {
        private int id;
        private int rate;

        public RateObject(int id, int rate) {
            this.id = id;
            this.rate = rate;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public int getRate() {
            return rate;
        }
    }

    public static interface RandomObject {
        public int getId();
        public int getRate();
    }

    public static <T extends RandomObject> T randomObject(List<T> list) {
        int totalRate = 0;
        for (T temp : list) {
            totalRate += temp.getRate();
        }
        int randomRate = rand(0, totalRate);
        int currRate = 0;
        T t = null;
        for (T temp : list) {
            currRate += temp.getRate();
            if (randomRate <= currRate) {
                t = temp;
                break;
            }
        }
        return t;
    }
    
	/**
	 * 随机产生n个不重复的int数字
	 * 
	 * @param min 最小值
	 * @param max 最大值
	 * @param n 随机数的数量n
	 * @param include 是否包含最小值最大值
	 * @return
	 */
	public static int[] randomNoRepeatNum(int min, int max, int n, boolean include) {
		int min1 = include ? min : min + 1;
		int max1 = include ? max + 1 : max;
		if ((max1 - min1 + (include ? 1 : 0)) < n) {
			throw new IllegalArgumentException("取值范围不足");
		}
		return ThreadLocalRandom.current().ints(min1, max1).distinct().limit(n).toArray();
	}
}

