package jpabook.jpashop.service;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.exception.NotEnoughStockException;
import jpabook.jpashop.repository.OrderRepository;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class OrderServiceTest {

    @Autowired
    EntityManager em;
    @Autowired
    OrderService orderService;
    @Autowired
    OrderRepository orderRepository;

    @Test
    public void 상품주문() throws Exception{
        // given
        Member member = createMember();
        Book book = createBook(10000, 10, "시골 JPA");

        int orderCount = 2;
        // when
        Long orderId = orderService.order(member.getId(), book.getId(), orderCount);

        // then
        Order getOrder = orderRepository.findOne(orderId);

        assertEquals("상품 주문시 상태는 ORDER", OrderStatus.ORDER, getOrder.getStatus());
        assertEquals("주문한 상품 종류 수가 정확해야 한다.", 1, getOrder.getOrderItems().size());
        assertEquals("주문 가격은 가격 * 수량이다.", 10000 * orderCount, getOrder.getTotalPrice());
        assertEquals("주문 수량만큼 재고가 줄어야 한다.", 8, book.getStockQuantity());
    }

    private Book createBook(int price, int stockQuantity, String name) {
        Book book = new Book();
        book.setName(name);
        book.setPrice(price);
        book.setStockQuantity(stockQuantity);
        em.persist(book);
        return book;
    }

    private Member createMember() {
        Member member = new Member();
        member.setName("회원1");
        member.setAddress(new Address("서울", "독산동", "1234-1234"));
        em.persist(member);
        return member;
    }

    @Test(expected = NotEnoughStockException.class)
    public void 상품주문재고수량초과() throws Exception{
        Member member = createMember();
        Item item = createBook(10000, 10, "시골 JPA");
        int orderCount = 11;

        orderService.order(member.getId(), item.getId(), orderCount);

        fail("재고 수량 부족 예외가 발생해야한다.");
    }

    @Test
    public void 주문취소() throws Exception{
        Member member = createMember();
        Item item = createBook(10000, 10, "JPA 신남");
        int orderCount = 2;
        Long orderId = orderService.order(member.getId(), item.getId(), orderCount);

        orderService.cancelOrder(orderId);

        Order getOrder = orderRepository.findOne(orderId);

        assertEquals("주문 상태는 CANCEL", OrderStatus.CANCEL, getOrder.getStatus());
        assertEquals("주문이 취소된 상품은 그만큼 재고가 증가해야하 한다.", 10, item.getStockQuantity());
    }
}