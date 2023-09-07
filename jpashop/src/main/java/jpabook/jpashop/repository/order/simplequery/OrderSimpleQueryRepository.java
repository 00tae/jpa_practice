package jpabook.jpashop.repository.order.simplequery;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;

import javax.persistence.EntityManager;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderSimpleQueryRepository {

    private final EntityManager em;
    // V3 보다 조금 더 성능 최적화 되어있음
    // 필드가 많거나, API 호출이 실시간으로 많을 경우 유리
    // API 스펙이 포함된다는 단점
    // 복잡한 쿼리인 경우 코드가 난잡할 수 있다는 단점
    public List<OrderSimpleQueryDto> findOrderDtos() {
        return em.createQuery(
                        "select new jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto(o.id, m.name, o.orderDate, o.status, d.address) " +
                                " from Order o" +
                                " join o.member m" +
                                " join o.delivery d", OrderSimpleQueryDto.class)
                .getResultList();
    }

    // V4 보다 성능 최적화를 위해서는
    // JPA 제공 네이티브 SQL이나 스프링 JDBC Template 사용
}
