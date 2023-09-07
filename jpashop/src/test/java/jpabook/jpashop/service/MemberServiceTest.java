package jpabook.jpashop.service;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class) // Junit 실행할때 스프링이랑 같이 사용하려고할 때
@SpringBootTest // 스프링부트안에서 실행할때, Autowired 사용 등
@Transactional // 테스트케이스에 있으면 기본적으로 rollback
public class MemberServiceTest {

    @Autowired MemberService memberService;
    @Autowired MemberRepository memberRepository;
    @Autowired EntityManager em;

    @Test
//    @Rollback(false)
    public void 회원가입() throws Exception{
        //given
        Member member = new Member();
        member.setName("kim");

        //when
        Long savedId = memberService.join(member);

        //then
        assertEquals(member, memberRepository.findOne(savedId));
    }

    @Test(expected = IllegalStateException.class)
    public void 중복_회원_예외() throws Exception{
        //given
        Member member = new Member();
        member.setName("kim");
        Member member2 = new Member();
        member2.setName("kim");

        //when
        Long savedId = memberService.join(member);
        Long savedId2 = memberService.join(member2);

        //then
        fail("예외가 발생해야 한다.");
    }
}