package concurrency

object RaceCondition extends App {

  // Race condition
  def runInParallel() = {
    var x = 0
    val thread1 = new Thread(() => {
      x = 1
    })
    val thread2 = new Thread(() => {
      x = 2
    })
    thread1.start()
    thread2.start()
    println(x) // 0, 1, 2 지맘대로임 아주
  }

//  for (_ <- 1 to 10000) runInParallel()

  // #1. use synchronized() at critical area

  // #2. use @volatile

  // Exercises
  // #1) 50 inception threads Thread1 -> Thread2 -> Thread3 ...
  //  runnable saying "hello from thread #?"
  // print in reverse order

  createNewThread(0)

  def createNewThread(x: Int): Option[Thread] = {
    if (x <= 50) {
      val thread = new Thread(() => println({
        s"hello from thread #$x"
      }))
      createNewThread(x + 1) match {
        case Some(thread) => {
          thread.start()
          thread.join()
        }
        case _ =>
      }
      Some(thread)
    } else {
      None
    }
  }

  def inceptionThreads(maxThreads: Int, i: Int = 1): Thread = new Thread(() => {
    if (i < maxThreads) {
      val newThread = inceptionThreads(maxThreads, i + 1)
      newThread.start()
      newThread.join() // join()을 쓰자! 끝날때까지 기다리는거
    }
    println(s"Hello from thread $i")
  })

  inceptionThreads(50).start()

  // 2. question
  var x = 0
  val threads = (1 to 100).map(_ => new Thread(() => x += 1))
  threads.foreach(_.start())
  threads.foreach(_.join())
  println(x)
  // possible max x => 100
  // possible min x = > 1 -> 100개 쓰레드 다같이 0으로 접근 1로 증대

  // 3. sleep Fallacy
  var message = ""
  val awesomeThread = new Thread(() => {
    Thread.sleep(1000)
    message = "scala is awesome"
  })

  message = "scala sucks"
  awesomeThread.start()
  Thread.sleep(900)
  println(message)

  // message는 뭘까? 맨날 그래? -> 대부분 scala is awesome 일텐데...
  // Thread sleep 이 100% 정확하지 않은걸로 알고있어서 제때 저 쓰레드가 OS로부터 할당 못받아서 Starvation 일어나면 덮어쓰기 안대지 않을수도?
  // sleeping 은 절때 thread의 실행순서를 보장하지 않아!

  // 그럼 어떻게 해결해??
  //  println(message) 하기 전에 thread.join() 을 해보자!!!!! 그러면 저거 실행이 끝날때 까지 기다려서 반영 뚝딱 댈거야
  // 3. sleep Fallacy - fixed
  var messageFixed = ""
  val awesomeThreadFixed = new Thread(() => {
    Thread.sleep(1000)
    messageFixed = "scala is awesome"
  })

  messageFixed = "scala sucks"
  awesomeThreadFixed.start()
  Thread.sleep(900)
  awesomeThreadFixed.join()
  println(messageFixed)
}
