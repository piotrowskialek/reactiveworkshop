package pl.orange.reactorworkshop;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.vavr.API.unchecked;

@Log4j2
public class MonoFluxTest {

    public void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Mono<Integer> findByIdRx(int id) {
        return Mono.fromCallable(() -> {
            log.info("Loading before sleep: {}", id);
            sleep(Duration.ofMillis(1000 * id));
            log.info("Loading after sleep: {}", id);
            return id;
        });
    }

    public int findById(int id) {
        log.info("Loading before sleep: {}", id);
        sleep(Duration.ofMillis(1000 * id));
        log.info("Loading after sleep: {}", id);
        return id;
    }


    @Test
    public void test1() {
        Flux.just(1, 2, 3, 4, 5, 6, 7, 8, 9) //Flux.range(1, 10)
                .subscribe(log::debug);
    }

    @Test
    public void test2() {
        Flux.just(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .map(unchecked(integer -> {
                    Thread.sleep(1000);
                    return integer;
                }))
                .subscribe(log::debug);
    }

    @Test
    public void test9() {
        Flux.range(0, 10)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(log::debug);
    }

    @Test
    @SneakyThrows
    public void test5() {
        Flux.range(0, 1000)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(log::debug);

        Flux.range(0, 1000)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(log::debug);

        TimeUnit.SECONDS.sleep(2);
    }

    @Test
    public void test7() {
        Flux.just(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .subscribe(log::debug);
    }

    @Test
    @SneakyThrows
    public void test3() {
        log.debug("START");
        Flux.range(0, 100) //zadania beda sie co 12 iteracji coraz dluzej wykonywc - dlaczego?
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .map(integer -> {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return integer;
                })
                .subscribe(log::debug);

        TimeUnit.SECONDS.sleep(15);
    }

    @Test
    @SneakyThrows
    public void test4() {
        findByIdRx(1)
                .publishOn(Schedulers.boundedElastic())
                .mergeWith(findByIdRx(2).publishOn(Schedulers.boundedElastic()))
                .parallel()
                .runOn(Schedulers.parallel())
                .subscribe(log::debug);

        TimeUnit.SECONDS.sleep(10);
    }

    @Test
    @SneakyThrows
    public void test8() {
        Flux.just("hello")

                .doOnNext(v -> System.out.println("just " + Thread.currentThread().getName()))

                .publishOn(Schedulers.boundedElastic())

                .doOnNext(v -> System.out.println("publish " + Thread.currentThread().getName()))

                .delayElements(Duration.ofMillis(500))

                .subscribeOn(Schedulers.single())

                .subscribe(v -> System.out.println(v + " delayed " + Thread.currentThread().getName()));


        TimeUnit.SECONDS.sleep(1);
    }

    final Flux<Integer> blockingCall(List<Integer> integers) {
        return Flux.fromIterable(integers)
                .map(integer -> findById(integer)); //nigdy tak nie rob, nie wolno mapowac elementow strumienia na blokujace calle,
        // trzeba je owrapowac w nowy strumien

//        return Flux.fromIterable(integers) //poprawne rozwiazanie
//                .flatMap(integer -> Mono.fromCallable(() -> findById(integer)).subscribeOn(Schedulers.boundedElastic()));

    }

    @Test
    @SneakyThrows
    public void test10() {
        blockingCall(List.of(1, 2, 3))
//                .subscribeOn(Schedulers.boundedElastic()) //pytanie: jak sie bedzie tu roznic wykonanie jak zamiast subscribe damy publish?
                .subscribe(body -> System.out.println(Thread.currentThread().getName() + " from first list, got " + body));

        blockingCall(List.of(4, 5))
//                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(body -> System.out.println(Thread.currentThread().getName() + " from second list, got " + body));

        TimeUnit.SECONDS.sleep(10);
    }

    @Test
    @SneakyThrows
    public void test11() {

        VirtualTimeScheduler virtualTimeScheduler = VirtualTimeScheduler.create();

        Mono<Integer> mono = Mono
                .delay(Duration.ofSeconds(5), virtualTimeScheduler)
                .timeout(Duration.ofSeconds(1), virtualTimeScheduler)
                .doOnError(log::debug)
                .retry(2)
                .map(x -> 100) //nie wykona sie nigdy
                .onErrorReturn(-1);

        StepVerifier
                .withVirtualTime(() -> mono, () -> virtualTimeScheduler, 1)
                .expectSubscription()
                .expectNoEvent(Duration.ofSeconds(3))
                .thenAwait(Duration.ofSeconds(2))
                .expectNext(-1)
                .expectComplete()
                .verify();

    }
}
