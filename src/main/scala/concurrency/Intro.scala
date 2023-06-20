package concurrency

import java.util.concurrent.Executors

object Intro extends App {

    // JVM에서의 쓰레드는 Thread class, Runnable interface로 구동 (Runnable은 함수형 인터페이스)
    /*
    * interface Runnable {
    *   public void run()
    * }
    * */

    val runnable = new Runnable {
        override def run(): Unit = println("쓰레드 병렬 처리")
    }

    val aThread = new Thread(runnable)

    aThread.start() // start() 호출 시, JVM은 OS 쓰레드를 생성하여 Thread 객체에 매핑하는 역할을 수행
    runnable.run() // 이런다고 쓰레드 되는거 아니야. 이건 그냥 객체 메서드 호출임
    aThread.join() // 쓰레드가 종료될 때까지 블럭킹

    val threadHello = new Thread(() => (1 to 5).foreach(_ => println("hello")))
    val threadGoodbye = new Thread(() => (1 to 5).foreach(_ => println("goodbye")))

    threadHello.start()
    threadGoodbye.start()

    // 쓰레드는 생성/삭제 비용이 진짜 커 => Executors로 쓰레드를 관리하자!
    val pool = Executors.newFixedThreadPool(10)

    // runnable 전달 시, 쓰레드 10개 중 하나가 이거 실행.
    // Executor가 쓰레드 생성/중지/삭제 담당하고, 우리는 액션만 넘겨주자
    pool.execute(() => println("Sth in thread pool"))

    pool.execute(() => {
        Thread.sleep(1000)
        println("done after 1 sec")
    })

    pool.execute(() => {
        Thread.sleep(1000)
        println("almost done")
        Thread.sleep(1000)
        println("done after 2 sec")
    })

    // Executors에 대해 중지를 요구할 수 있음
    pool.shutdown() // 이제 쓰레드가 처리할 액션 그만 받아!
    println(pool.isShutdown) // True

    // shutDown 하고나서 액션을 요구하면 Executor에 액션 넘겨주는 Calling Thread (여기서는 Main Thread)에서 예외 발생
    pool.execute(() => println("after shutdown")) // Exception in thread "main" java.util.concurrent.RejectedExecutionException

    // Sleeping Thread 인터럽트를 걸어버려 예외 발생
    pool.shutdownNow() // java.lang.InterruptedException: sleep interrupted
}
