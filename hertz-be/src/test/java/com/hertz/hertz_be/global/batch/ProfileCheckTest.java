//package com.hertz.hertz_be.global.batch;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.mock.env.MockEnvironment;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.util.Arrays;
//
//@ActiveProfiles("test")
//public class ProfileCheckTest {
//
//    @Value("${custom.test.value}")
//    private String checkValue;
//
//    @Test
//    void checkActiveProfiles() {
//        MockEnvironment mockEnv = new MockEnvironment();
//        mockEnv.setActiveProfiles("test");
//
//        System.out.println("✅ Active Profiles: " + Arrays.toString(mockEnv.getActiveProfiles()));
//    }
//
//    @Test
//    void checkPropertyValue() {
//        String value = checkValue;
//        System.out.println("🧪 custom.test.value = " + checkValue);
//        assert value.equals("HELLO_TEST");
//    }
//
////    @Test
////    void checkActiveProfiles() {
////        System.out.println("✅ Active Profiles: " + Arrays.toString(env.getActiveProfiles()));
////    }
//}