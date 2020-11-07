// << +1 || >> -1
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.*;
import java.util.Scanner;
import java.io.FileWriter;
import java.util.function.Function;
import java.util.stream.Collectors;

public class  ExtHash<K,V> {
    static class Bucket<K, V> {
        int localdepth = 0;
        int size = 0;
        static int bucket_size = 100;
        private MyHashMap bucket = new MyHashMap<K, V>(size);
        List<K> keyset = new ArrayList<K>();

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

     public ExtHash() {
        bucketlist.add(new Bucket<K, V>());
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
        //System.out.println(hd & (1 << globaldepth.get()) - 1);
        //System.out.println(hd);
        //Bucket<K,V> b = bucketlist.get((int) (hd & (1 << globaldepth.get()) - 1));
        //System.out.println(hd);
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
            Bucket<K, V> b1 = new Bucket<K, V>();
            Bucket<K, V> b2 = new Bucket<K, V>();

            //System.out.println(b.keyset);

            for (K key2 : b.keyset) {
                V value2 = (V) b.bucket.getData(key2);
                Data<K, V> d2 = new Data<K, V>(key2, value2);

                //long hd = Long.parseLong(hashcode(key2));
                //int hashcode =(int) (hd & ((1 << globaldepth.get())) - 1);

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

    public int countElements(){
        for(int i=0; i<bucketlist.size(); i++){
                counter += bucketlist.get(i).keyset.size();
            }
        return counter;
        }

    public static void main(String[] args) throws IOException, InterruptedException {
        //this example is from anadiotis slides..
        //when we have the dataset we can split it to as many threads we want
        //we have to test the code a bit more
        //overleaf (?)

        ExtHash<String, String> eh2 = new ExtHash<String, String>();
        ArrayList<Long> times = new ArrayList<>();

        Scanner reader = new Scanner(new File("911.csv"));
        // Skip the 2 first lines
        //reader.nextLine();
        //reader.nextLine();
        String sep = ",";

        while(reader.hasNextLine()) {
            String [] line = reader.nextLine().split(sep);
            long start = System.nanoTime();
            eh2.put(line[2], line[4]);
            eh2.getValue(line[2]);
            long finish = System.nanoTime();
            times.add(finish - start);
        }
        //System.out.println("Time=" + times);

        FileWriter writer = new FileWriter("Access911Bsize100.txt");
        writer.write("Time=" + times);
        writer.close();
        //System.out.println(eh2);
        //System.out.println(eh2.getValue("\"Metadata for Digital Media: Introduction to the Special Issue\""));
        //System.out.println("Insertion "+linetype+" finished");
    }
}


