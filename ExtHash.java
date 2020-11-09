// << +1 || >> -1
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.*;
import java.util.Scanner;
import java.io.FileWriter;

public class  ExtHash<K,V> {
    static class Bucket<K, V> {
        int localdepth = 0;
        int size = 0;
        int bucket_size;
        private MyHashMap bucket = new MyHashMap<K, V>(size);
        List<K> keyset = new ArrayList<K>();

        public Bucket(int bucket_size){
            this.bucket_size = bucket_size;
        }

        public void put(Data<K, V> x) {
            // put data in bucket and create keyset
            if (!(keyset.contains(x.key))) {
                keyset.add(x.key);
                //System.out.println(keyset);
            }
            bucket.addData(x);
        }

        public V get(K key) {
            return (V) bucket.getData(key);
        }

        public boolean isFull() {
            return bucket.getSize() >= bucket_size;
        }

        public int getSize() {
            return bucket_size;
        }

        @Override
        public String toString() {
            return "{ bucket=" + bucket + ",size= " + bucket.getSize() + ", localdepth=" + localdepth + "}\n";
        }
    }

    AtomicInteger globaldepth = new AtomicInteger(0);
    List<Bucket<K, V>> bucketlist = new ArrayList<Bucket<K, V>>();
    int counter = 0;

     public ExtHash(int bucket_size) {
        bucketlist.add(new Bucket<K, V>(bucket_size));
    }

     public static <K> String hashcode(K k) {
        // convert key to binary and return it. for now it only work with integer keys.
        // if we will have string keys as well we have to find a way to convert string to integers
        String hashcode = Integer.toBinaryString(k.hashCode());
        //System.out.println(k + "," + hashcode);
        return hashcode;
    }

     public Bucket<K, V> getBucket(K key) { // get bucket based on hashcode(key)
        String hashcode = hashcode(key);
        BigInteger hd = new BigInteger(hashcode);
        hd = (hd.and(BigInteger.valueOf(1 << globaldepth.get()).subtract(BigInteger.valueOf(1))));
        //System.out.println(hd);
        Bucket<K, V> b = bucketlist.get(hd.intValue());
        return b;
    }

     public V getValue(K key) {
        String hashcode = hashcode(key);
        BigInteger hd = new BigInteger(hashcode);
        hd = (hd.and(BigInteger.valueOf(1 << globaldepth.get()).subtract(BigInteger.valueOf(1))));
        Bucket<K, V> b = bucketlist.get(hd.intValue());
        for (int i = 0; i < b.getSize(); i++) {
            if (b.bucket.getData(key) != null) {
                return (V) b.get(key); //(V) is called casting.
            }
        }
        return null;
    }

     public void remove(K key) {
        String hashcode = hashcode(key);
        BigInteger hd = new BigInteger(hashcode);
        hd = (hd.and(BigInteger.valueOf(1 << globaldepth.get()).subtract(BigInteger.valueOf(1))));
        Bucket<K, V> b = bucketlist.get(hd.intValue());
        for (int i = 0; i < b.getSize(); i++) {
            if (b.bucket.getData(key) != null) {
                b.bucket.remove(key); //(V) is called casting.
            }
        }
    }

     public void put(K key, V value) {
        Bucket<K, V> b = getBucket(key);
        //System.out.println(b);
        if (b.localdepth == globaldepth.get() && b.isFull()) {
            //in this case we double the buckets and we increase globaldepth.
            List<Bucket<K, V>> t2 = new ArrayList<Bucket<K, V>>(bucketlist);
            bucketlist.addAll(t2);
            globaldepth.incrementAndGet();
        }

        if (b.localdepth < globaldepth.get() && b.isFull()) {
            // in this case we dont have to double the no of buckets.. we just have to split the current bucket because its full..
            Data<K, V> d = new Data<K, V>(key, value);
            b.put(d);
            //split data of bucket b to buckets b1 and b2
            Bucket<K, V> b1 = new Bucket<K, V>(b.bucket_size);
            Bucket<K, V> b2 = new Bucket<K, V>(b.bucket_size);

            //System.out.println(b.keyset);

            for (K key2 : b.keyset) {
                V value2 = (V) b.bucket.getData(key2);
                Data<K, V> d2 = new Data<K, V>(key2, value2);
                String hashcode = hashcode(key2);
                BigInteger hd = new BigInteger(hashcode);
                hd = (hd.and(BigInteger.valueOf(1 << globaldepth.get()).subtract(BigInteger.valueOf(1))));

                //System.out.println(hd);
                if (hd.or(BigInteger.valueOf(1 << b.localdepth)).equals(hd)) {
                    b2.put(d2);
                } else {
                    b1.put(d2);
                }
            }

            List<Integer> l = new ArrayList<Integer>();
            for (int i = 0; i < bucketlist.size(); i++) {
                if (bucketlist.get(i) == b) {
                    l.add(i);
                }
            }

            for (int i : l) {
                if ((i | (1 << b.localdepth)) == i) {
                    bucketlist.set(i, b2);
                } else {
                    bucketlist.set(i, b1);
                }
            }
            b1.localdepth = b.localdepth + 1;
            b2.localdepth = b.localdepth + 1;

        } else {
            //if the bucket in not full just add the data.
            Data<K, V> d = new Data<K, V>(key, value);
            b.put(d);
        }
    }

    @Override
    public String toString() {
        return "ExtHash{" +
                "globaldepth=" + globaldepth +
                ",\n " + bucketlist + '}';
    }

    public static void writeInsertPerformance(String filename, int bucket_size) throws IOException {
        ExtHash<String, String> eh2 = new ExtHash<String, String>(bucket_size);
        ArrayList<Long> times = new ArrayList<>();

        Scanner reader = new Scanner(new File("911.csv"));
        reader.nextLine();
        String sep = ",";
        int i = 0;

        while(reader.hasNextLine()) {
            String [] line = reader.nextLine().split(sep);
            i++;
            if((i-1) % 5000 == 0) {
                MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
                System.gc();
                //MemoryUsage start = mbean.getHeapMemoryUsage();
                long start = System.nanoTime();
                eh2.put(line[2], line[4]);
                long finish = System.nanoTime();
                //System.gc();
                //MemoryUsage finish = mbean.getHeapMemoryUsage();
                //long memory = finish.getUsed() - start.getUsed();
                times.add(finish - start);
            }else{
                eh2.put(line[2], line[4]);
            }
        }

        PrintWriter pw=new PrintWriter(new FileWriter(filename));
        pw.println(times.toString());
        pw.close();
    }

    public static void writeAccessPerformance(String filename, int bucket_size) throws IOException {
        ExtHash<String, String> eh2 = new ExtHash<String, String>(bucket_size);
        ArrayList<Long> times = new ArrayList<>();
        ArrayList<String> keys = new ArrayList<>();

        Scanner reader = new Scanner(new File("911.csv"));
        reader.nextLine();
        String sep = ",";
        int i = 0;

        while(reader.hasNextLine()) {
            String [] line = reader.nextLine().split(sep);
            keys.add(line[2]);
            eh2.put(line[2], line[4]);
            i++;
            if((i-1) % 5000 == 0) {
                long countTime = 0;
                int keyIndex = (int)(Math.random() * (i-1));
                long start = System.nanoTime();
                eh2.getValue(keys.get(keyIndex));
                //Runtime runtime = Runtime.getRuntime();
                //runtime.gc();
                //long memory = runtime.totalMemory() - runtime.freeMemory();
                long finish = System.nanoTime();
                countTime += finish - start;
                times.add(countTime);
            }
        }

        PrintWriter pw = new PrintWriter(new FileWriter(filename));
        pw.println(times.toString());
        pw.close();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int[] bucket_sizes = {25, 100, 500, 1000, 1500};

        for (int i : bucket_sizes) {
            String filename = "New_Memory_Insertion_" + String.valueOf(i);
            String afilename = "NewMemoryAccess_" + String.valueOf(i);
            writeInsertPerformance(filename, i);
            writeAccessPerformance(afilename, i);
        }
    }
}


