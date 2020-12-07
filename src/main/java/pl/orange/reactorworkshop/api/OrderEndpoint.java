package pl.orange.reactorworkshop.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.orange.reactorworkshop.api.dto.OrderDto;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@RequestMapping("/orders")
class OrderEndpoint {

    @GetMapping("/{id}")
    Mono<OrderDto> getOrder(@PathVariable String id) {
        return Mono.just(new OrderDto(id, Instant.now()));
    }
}
