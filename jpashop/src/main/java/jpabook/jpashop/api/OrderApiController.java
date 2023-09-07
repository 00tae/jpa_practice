package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.query.OrderFlatDto;
import jpabook.jpashop.repository.order.query.OrderItemQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@RestController
@RequiredArgsConstructor
public class OrderApiController {
    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    /**
     * 조회 방식 권장 순서
     * 1. 엔티티 조회 방식
     *   1.1 페치 조인으로 쿼리 수 최적화
     *   1.2 컬렉션 최적화
     *      - 페이징 필요 > hibernate.default_batch_fetch_size or @BatchSize 로 최적화
     *      - 페이징 불필요 > 페치 조인
     * 2. DTO 조회 방식
     * 3. NativeSQL or 스프링 JdbcTemplate
     *
     * 엔티티 조회방식을 먼저 권장 하는 이유는
     * DTO 직접 조회 방식은 성능 최적화하거나 성능 최적화 방식을 변경할 때 많은 코드를 변경해야함
     *
     * 대부분 엔티티 조회 방식에서 성능 최적화만으로 해결됨
     * DTO 직접 조회 방식이 필요한 수준이라면 캐싱을 해야한다고 생각함
     *
     * 캐싱 > 엔티티를 직접 캐싱하면안됨, 영속성 컨텍스트 관리하고 있기 때문에 엉킬 수 있음
     *       엔티티 - DTO변환 - 캐싱
     */

    //-------------------------------------여기부터 엔티티 조회 방식-------------------------------------

    // 엔티티를 조회해서 그대로 반환 (사용하면 안됨)
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1(){
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName();
            order.getDelivery().getAddress();
            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(o -> o.getItem().getName());
        }
        return  all;
    }

    // 엔티티 조회 후 DTO 변환 (성능 안좋음)
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2(){
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        List<OrderDto> collect = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return collect;
    }

    // 페치 조인으로 쿼리 수 최적화 (성능 좋으나 페이징 안됨)
    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3(){
        List<Order> orders = orderRepository.findAllWithItem();
        List<OrderDto> collect = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return collect;
    }

    // 컬렉션 페이징과 한계 돌파
    // 컬렉션은 페치 조인시 페이징 불가능
    // ToOne 관계는 페치 조인으로 최적화하고
    // 컬렉션은 페치 조인 대신에 지연 로딩을 유지하고, hibernate.default_batch_fetch_size or @BatchSize 로 최적화
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(@RequestParam(value = "offset", defaultValue = "0") int offset,
                                        @RequestParam(value = "limit", defaultValue = "100") int limit){
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);

        List<OrderDto> collect = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return collect;
    }

    //-------------------------------------여기부터 DTO 직접 조회 방식-------------------------------------

    // JPA에서 DTO를 직접 조회
    // 코드 단순
    // 유지보수가 쉽다
    // 단건 조회할 때 유용함
    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4(){
        return orderQueryRepository.findOrderQueryDtos();
    }

    // 컬렉션 조회 최적화 - 일대다 관계인 컬렉션은 IN 절을 활용해서 메모리에 미리 조회해서 최적화
    // 코드 복잡
    // V4 의 N+1 문제 해결되기 때문에 성능이 좋음
    // 데이터 건수가 다수 일 때 유용함
    // 페이징 해야할 때
    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5(){
        return orderQueryRepository.findAllByDto_optimization();
    }

    // 플랫 데이터 최적화 - JOIN 결과를 그대로 조회 후 애플리케이션에서 원하는 모양으로 직접 변환
    // V5에서 쿼리 최적화
    // 데이터가 많으면 중복 전송이 증가해서 V5와 비교해서 성능 차이도 미미함
    // Order를 기준으로 페이징이 불가능 함
    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> ordersV6(){
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();
        return flats.stream()
                .collect(groupingBy(o -> new OrderQueryDto(o.getOrderId(), o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                        mapping(o -> new OrderItemQueryDto(o.getOrderId(), o.getItemName(), o.getOrderPrice(), o.getCount()), toList())
                )).entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(), e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(), e.getKey().getAddress(), e.getValue()))
                .collect(toList());
    }

    @Data
    static class OrderDto{

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order){
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            order.getOrderItems().stream().forEach(o -> o.getItem().getName());
            orderItems = order.getOrderItems().stream()
                    .map(o -> new OrderItemDto(o))
                    .collect(toList());
        }

        public OrderDto(Long id, String name, LocalDateTime orderDate, OrderStatus orderStatus, Address address, List<OrderItemDto> orderItems){
            this.orderId = id;
            this.name = name;
            this.orderDate = orderDate;
            this.orderStatus = orderStatus;
            this.address = address;
            this.orderItems = orderItems;
        }
    }

    @Data
    static class OrderItemDto{
        private String itemName;
        private int orderPrice;
        private int count;

        public OrderItemDto(OrderItem orderItem){
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }
}
