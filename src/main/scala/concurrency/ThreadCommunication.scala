package concurrency

import scala.collection.mutable
import scala.util.Random

object ThreadCommunication extends App {

  // Producer - Consumer problem
  // producer -> [?] -> consumer (consumer가 producer 끝나야 제대로 일할 수 있는데, 이걸 제대로 알려주려면 쓰레드 순서가 중!)

  // Synchronized -> 공유하고자 하는 자원에 대해 Synchronized를 걸어주면 JVM이 모니터를 뚝딱 걸어준다는거여 ㄷㄷ
  // - Entering Synchronized expression on an object LOCKS the object
  /*
  * val someObject = "hello"
  *
  * someObject.synchronized {  <- Locks the object's monitor (monitor: JVM에서 어떤 object가 어떤 thread에 의해 락 되었는지)
  *   // code
  * } <- release Lock
  *
  * Reference Type만 synchronized expression 쓸 수 있음
  *
  * - Synchronized 쓸때...
  *   - 누가 먼저 lock 취득하는지 assumption 하지 마세요
  *   - lock은 최소 범위/단위로만 사용하세요
  *   - thread safety를 가장 최선으로 신경쓰세요!
  *
  *
  * wait() & notify() -> Busy waiting을 벗어나 보다 효율적으로 각 쓰레드간 소통!
  * - synchronized expression 안에서만 쓸 수 있음!
  *  - wait()
  *   val someObject = "hello"
  *   someObject.synchronized { <- 모니터 lock 해버림
  *     // code part 1
  *     someObject.wait() <- lock을 해제하고 기다린다
  *     // code part 2 <- proceed 가능하다면 모니터 lock 다시 하고 마저 진행
  *   }
  *
  * - notify()
  *   someObject.synchronized { <- 모니터 lock 해버림
  *     // code part 1
  *     someObject.notify() <- "하나의" 자고 있는 쓰레드에게 마저 진행해도 좋다고 signal (notifyAll() 이면 싹다 동원가능)
  *     // code part 2
  *   } <- 하지만 내가 다 끝나고 나서야 진행할 수 있도록 함
  * */

  class SimpleContainer {
    private var value: Int = 0

    def isEmpty: Boolean = value == 0

    def get: Int = {
      val result = value
      value = 0
      result
    }

    def set(newValue: Int): Unit = {
      value = newValue
    }

  }

  def smartProdCons(): Unit = {
    val container = new SimpleContainer

    val consumer = new Thread(() => {
      println("[consumer] waiting...")
      container.synchronized {
        container.wait()
      }
      // container must have value! Producer can only wake me up!
      println("[consumer] I have consumed! " + container.get)
    })

    val producer = new Thread(() => {
      println("[producer] Hard at work...")
      Thread.sleep(2000)
      val value = 42
      container.synchronized {
        println("[producer] I'm producing " + value)
        container.set(value)
        container.notify()
      }
    })

    consumer.start()
    producer.start()
  }

  //  smartProdCons()

  // 2) 이번엔 Queue로 한번 해봅시다 => 아 재밌다 :)
  def prodConsLargeBuffer(): Unit = {
    val buffer: mutable.Queue[Int] = new mutable.Queue[Int]
    val capacity = 3

    val consumer = new Thread(() => {
      val random = new Random()

      while (true) {
        buffer.synchronized {
          if (buffer.isEmpty) {
            println("[consumer] buffer empty, waiting...")
            buffer.wait() // 없으면 대기하세요. 이거 notify 시그널 받을때 까지 다음 코드블럭으로 넘어가지 마세요!
          }

          val x = buffer.dequeue()
          println("[consumer] consumed " + x)

          buffer.notify() // producer야. 나 consume 했으니까, 혹시 너 버퍼 꽉차서 대기중이면 다시 일해라!
        }
        Thread.sleep(random.nextInt(500))
      }
    })

    val producer = new Thread(() => {
      val random = new Random()
      var i = 0

      while (true) {
        buffer.synchronized {
          if (buffer.size == capacity) {
            println("[producer] buffer full, waiting...")
            buffer.wait()
          }

          println("[producer] producing " + i)
          buffer.enqueue(i)

          buffer.notify() // 그 consumer 친구야. 혹시 한개도 없어서 기다리는 중이였다면, 지금 하나 넣었으니까 확인해볼래?

          i += 1
        }
        Thread.sleep(random.nextInt(1250))
      }
    })

    consumer.start()
    producer.start()
  }

  //  prodConsLargeBuffer()

  /*
  * Producer-Consumer Level3
  * producer1 --> [?, ?, ?, ?] -->
  * */

  def prodConsLargeBufferMultiple(consumerNumber: Int, producerNumber: Int): Unit = {
    val buffer: mutable.Queue[Int] = new mutable.Queue[Int]
    val capacity = 10000000

    val consumers: Seq[Thread] = (1 to consumerNumber).map { consumerNo =>
      new Thread(() => {
        val random = new Random()
        while (true) {
          buffer.synchronized {
            while (buffer.isEmpty) {
              println(s"[consumer$consumerNo] buffer is empty. Waiting...")
              buffer.wait()
            }

            val x = buffer.dequeue()
            println(s"[consumer$consumerNo] consumed $x")

            buffer.notify() // 이 시그널이 producer한테만 가지 않을거같은데?
          }
          Thread.sleep(random.nextInt(50))
        }
      })
    }

    val producers: Seq[Thread] = (1 to producerNumber).map { producerNo =>
      new Thread(() => {
        val random = new Random()
        var i = 100 * producerNo

        while (true) {
          buffer.synchronized {
            while (buffer.size == capacity) {
              println(s"[producer$producerNo] buffer full, waiting...")
              buffer.wait()
            }

            println(s"[producer$producerNo] producing " + i)
            buffer.enqueue(i)

            buffer.notify() // 그 consumer 친구야. 혹시 한개도 없어서 기다리는 중이였다면, 지금 하나 넣었으니까 확인해볼래?

            i += 1
          }
          Thread.sleep(random.nextInt(50))
        }
      })
    }

    consumers.foreach(_.start())
    producers.foreach(_.start())

  }

  //  prodConsLargeBufferMultiple(10000, 1000)

  // Exercise
  // 1) NotifyAll은 그럼 언제 쓰는거야?
  def testNotifyAll(): Unit = {
    val bell = new Object

    (1 to 100).foreach(i => new Thread(() => {
      bell.synchronized {
        println(s"[thread$i]: waiting...")
        bell.wait()
        println(s"[thread$i]: hooray!")
      }
    }).start())

    new Thread(() => {
      Thread.sleep(2000)
      println("[announcer] Rock'n Roll!")
      bell.synchronized {
//        bell.notify()
        bell.notifyAll()
      }
    }).start()
  }

//  testNotifyAll()

  // 2) Make deadlock -> 서로 다른 쓰레드가 서로 각각에 대해 락 걸어버리는 경우
  // 3) Make livelock -> 블락된것은 없는데 그 다음으로 서로 못넘어가는 경우. (계속 서로 양보하고 자고)
}
