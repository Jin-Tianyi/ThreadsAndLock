package com.jty.theads;

import org.junit.Test;

import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadsAndLock {
    public static void main(String[] args) {
        System.out.println("hello world!");
    }

    @Test
    public void CallableTest() {
        UserThread ut1 = new UserThread("线程A");
        UserThread ut2 = new UserThread("线程B");
        UserThread ut3 = new UserThread("线程C");

        //创建执行服务，三个线程
        ExecutorService es = Executors.newFixedThreadPool(3);
        try {
            //提交线程任务
            Future future1 = es.submit(ut1);
            //获取线程执行结果
            System.out.println(future1.get());
            Future future2 = es.submit(ut2);
            System.out.println(future2.get());
            Future future3 = es.submit(ut3);
            System.out.println(future3.get());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            es.shutdown();
        }
    }

    class UserThread implements Callable<String> {
        private String threadName;
        private int index;
        private static final int LOOP_NUM = 10;

        public UserThread() {
            this.threadName = "";
        }

        public UserThread(String name) {
            this.threadName = name;
            this.index = 0;
        }

        @Override
        public String call() throws Exception {
            while (index < LOOP_NUM) {
                //每次打印休眠0.1毫秒
                Thread.sleep(100);
                System.out.println(this.threadName + "执行次数:" + this.index++);
            }
            return this.threadName + " end";
        }
    }

    /**
     * 测试线程状态
     * NEW 尚未启动的线程的线程状态。
     * RUNNABLE 可运行线程的线程状态。 处于可运行状态的线程正在Java虚拟机中执行，但它可能正在等待来自操作系统的其他资源，例如处理器。
     * BLOCKED 线程的线程状态被阻塞等待监视器锁定。 处于阻塞状态的线程正在等待监视器锁定以在调用Object.wait之后输入同步块/方法或重新输入同步块/方法。。
     * WAITING 等待线程的线程状态。 由于调用以下方法之一，线程处于等待状态：
     * Object.wait没有超时
     * Thread.join没有超时
     * LockSupport.park。
     * TIMED_WAITING 具有指定等待时间的等待线程的线程状态。 由于在指定的正等待时间内调用以下方法之一，线程处于定时等待状态：
     * Thread.sleep
     * Object.wait超时
     * Thread.join超时
     * LockSupport.parkNanos
     * LockSupport.parkUntil
     * TERMINATED 终止线程的线程状态。 线程已完成执行。
     */
    @Test
    public void ThreadState() throws InterruptedException {
        Stock stock = new Stock(20);
        Thread t1 = new Thread(() -> {
            try {
                //休眠0.5毫秒，让主线程检测到阻塞状态
                Thread.sleep(500);
                //添加20个库存
                for (int i = 0; i < 20; i++) {
                    stock.addStock();
                    if (Thread.currentThread().getState() != Thread.State.TERMINATED) {
                        System.out.println("线程未结束" + Thread.currentThread().getState());
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        System.out.println(t1.getState());
        t1.start();
        //监控t线程的sleep状态
        while (t1.getState() != Thread.State.TERMINATED) {
            Thread.sleep(100);
            System.out.println("线程阻塞：" + t1.getState());
        }
        //休眠1毫秒保证在t线程之后结束，获取到stock中的结果
        Thread.sleep(1000);
        System.out.println("线程结束：" + t1.getState());
        if (t1.getState() == Thread.State.TERMINATED) {
            for (int i = 0; i < stock.getRemain(); i++) {
                System.out.println(stock.reduceStock());
            }
        }
    }

    /**
     * priority 越大，优先级越高
     * MAX_PRIORITY = 10
     * MIN_PRIORITY = 1
     * 优先级高的不一定越快得到执行，看cpu的调度
     *
     * @throws InterruptedException
     */
    @Test
    public void ThreadPriority() throws InterruptedException {
        Thread t1 = new Thread(() -> {
            System.out.println("t1..........");
        });
        t1.setPriority(2);
        t1.start();
        Thread t2 = new Thread(() -> {
            System.out.println("t2..........");
        });
        t2.setPriority(Thread.MAX_PRIORITY);
        t2.start();
    }

    /**
     * synchronized (stock){...} 执行代码块时会对stock对象实例加锁，避免其他线程修改stock实例
     * 注意：
     * synchronized (obj){...} obj实例必须在代码块中
     * <p>
     * synchronized type function(...){...} 同步方法锁定this实例。
     * 相当于
     * <p>
     * type function(...){
     * synchronized (this){...}
     * }
     *
     * @throws InterruptedException
     */
    @Test
    public void ThreadSynchronized() throws InterruptedException {
        //初始化库存数量
        Stock stock = new Stock(10);
        //测试同步块
       /* new Thread(() -> {
            synchronized (stock) {

                while (stock.getRemain() > 0) {
                    try {
                        //休眠以便其他线程能被调用，放大问题出现概率
                        Thread.sleep(10);
                        //添加同步块控制线程同步
                        //减少库存数量
                        int i = stock.reduceStock();
                        System.out.println("t1-------- " + i);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        new Thread(() -> {
            synchronized (stock) {

                while (stock.getRemain() > 0) {
                    try {
                        //休眠以便其他线程能被调用，放大问题出现概率
                        Thread.sleep(10);
                        //添加同步块控制线程同步
                        //减少库存数量
                        int i = stock.reduceStock();
                        System.out.println("t1-------- " + i);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        new Thread(() -> {
            synchronized (stock) {

                while (stock.getRemain() > 0) {
                    try {
                        //休眠以便其他线程能被调用，放大问题出现概率
                        Thread.sleep(10);
                        //添加同步块控制线程同步
                        //减少库存数量
                        int i = stock.reduceStock();
                        System.out.println("t1-------- " + i);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();*/

        //测试同步方法
        new Thread(() -> {
            Lock lock = new ReentrantLock();
            while (stock.getRemain() > 0) {
                try {
                    //休眠以便其他线程能被调用，放大问题出现概率
                    Thread.sleep(10);
                    //减少库存数量
                    lock.lock();
                    int i = stock.reduceStock();
                    System.out.println("t1------- " + i);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    lock.unlock();
                }
            }
        }).start();
        new Thread(() -> {
            Lock lock = new ReentrantLock();
            while (stock.getRemain() > 0) {
                try {
                    //休眠以便其他线程能被调用，放大问题出现概率
                    Thread.sleep(10);
                    //减少库存数量
                    lock.lock();
                    int i = stock.reduceStock();
                    System.out.println("t2------- " + i);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    lock.unlock();
                }
            }
        }).start();
        new Thread(() -> {
            Lock lock = new ReentrantLock();
            while (stock.getRemain() > 0) {
                try {
                    //休眠以便其他线程能被调用，放大问题出现概率
                    Thread.sleep(10);
                    //减少库存数量
                    lock.lock();
                    int i = stock.reduceStock();
                    System.out.println("t3------- " + i);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    lock.unlock();
                }
            }
        }).start();
        //休眠Main线程，以便能观察到其他线程的打印
        Thread.sleep(20000);
    }

    class Stock {
        /*数量 使用volatile修饰*/
        private  int number;

        public Stock(int number) {
            this.number = number;
        }

        /*添加库存*/
        public void addStock() {
            //增加一个
            this.number=this.number+1;
        }

        /*减少库存*/
        public int reduceStock() {
            //数目大于0则减少一个
            if (this.number > 0) {
                return (this.number=this.number-1);
            } else return 0;
        }

        public int getRemain() {
            return this.number;
        }
    }

    /**
     * 生产者与消费者：
     * 多个线程之间通过.wait()、notify()、notifyAll()来进行通信；通过某个条件如flag=false或size=10等（管程法和信号灯法）；来使当前线程等待以及唤醒其他线程；
     * 注意点：
     * synchronized用于在线程中锁定代码块时，wait()、notify()、notifyAll()方法必须在被锁定的代码块中；否则出现IllegalMonitorStateException异常，不是对象监视器的所有者；
     * synchronized用于锁定方法，则wait()、notify()、notifyAll()方法必须在被锁定的方法中
     * 线程以三种方式之一成为对象监视器的所有者：
     * <p>
     * 通过执行该对象的同步实例方法。
     * 通过执行在对象上同步的synchronized语句的主体。
     * 对于类型为Class,的对象，通过执行该类的同步静态方法。
     *
     * @throws InterruptedException
     */
    @Test
    public void ThreadsCommunication() throws InterruptedException {
        MyStock stock = new MyStock(20);
        new Thread(() -> {
            //编号计数
            int i = 1;
            try {
                while (true) {
                    synchronized (stock) {
                        //库存是满的就等待
                        if (stock.isFull()) {
                            stock.wait();
                        } else {
                            //库存不满就添加库存
                            stock.addStock("SHOPPING-NO." + i++);
                            //打印当前库存余量
                            System.out.println(Thread.currentThread().getName() + "库存->" + stock.getRemain());
                            //唤醒消费者
                            stock.notifyAll();
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }, "店主").start();
        new Thread(() -> {
            try {
                while (true) {
                    //制造休眠让另一个消费者有时间消费
                    Thread.sleep(200);
                    synchronized (stock) {
                        //库存是空的就等待添加库存
                        if (stock.isEmpty()) {
                            stock.wait();
                        } else {
                            //有库存，消费者购买减少一个库存
                            System.out.println(Thread.currentThread().getName() + "消费->" + stock.reduceStock());
                        }
                        //唤醒生产者和另外消费者
                        stock.notifyAll();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "客户 1").start();
        new Thread(() -> {
            try {
                while (true) {
                    //制造休眠让另一个消费者有时间消费
                    Thread.sleep(200);
                    synchronized (stock) {
                        //库存是空的就等待添加库存
                        if (stock.isEmpty()) {
                            stock.wait();
                        } else {
                            //有库存，消费者购买减少一个库存
                            System.out.println(Thread.currentThread().getName() + "消费->" + stock.reduceStock());
                            //唤醒生产者和另外消费者
                            stock.notifyAll();
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }, "客户 2").start();

        //休眠Main线程，以便能观察到其他线程的打印
        Thread.sleep(20000);

    }
}


class MyStock {
    private LinkedList ls;
    private int size;

    public MyStock(int size) {
        this.size = size;
        this.ls = new LinkedList();
    }

    /*添加库存*/
    public void addStock(String code) {
        //向后添加一个库存商品
        this.ls.offer(code);
    }

    /*减少库存*/
    public String reduceStock() {
        //获取并删除第一个存入商品
        String code = (String) this.ls.poll();
        return code;
    }

    public boolean isFull() {
        return this.ls.size() == this.size;
    }

    public boolean isEmpty() {
        return this.ls.size() == 0;
    }

    public int getSize() {
        return size;
    }

    public int getRemain() {
        return this.ls.size();
    }
}