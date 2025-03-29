package com.supos.adapter.mqtt;

//import com.fasterxml.jackson.core.JsonParser;
//import com.fasterxml.jackson.databind.*;
//import com.fasterxml.jackson.databind.cfg.DeserializerFactoryConfig;
//import com.fasterxml.jackson.databind.deser.BeanDeserializer;
//import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
//import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory;
//import lombok.Data;
//
//import java.io.IOException;

//public class JsonObjectFactoryTest {
//    // 自定义反序列化工厂
//    class CustomDeserializerFactory extends BeanDeserializerFactory {
//        public CustomDeserializerFactory(DeserializerFactoryConfig config) {
//            super(config);
//        }
//
//        public JsonDeserializer<Object> createBeanDeserializer(DeserializationContext ctxt,
//                                                               JavaType type, BeanDescription beanDesc) throws JsonMappingException {
//            if (type.getRawClass() == Person.class) {
//                return new PersonDeserializer(this);
//            }
//            return super.createBeanDeserializer(ctxt,type, beanDesc);
//        }
//    }
//    // 自定义反序列化器
//    class PersonDeserializer extends BeanDeserializer {
//        public PersonDeserializer(BeanDeserializerFactory factory) {
//            super(factory);
//        }
//
//        @Override
//        public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
//            // 自定义创建逻辑
//            Person person = new Person();
//            person.setName("Custom " + p.getValueAsString());
//            return person;
//        }
//    }
//    @Data
//    public static class Person {
//        private String name;
//        private int age;
//
//        @Override
//        public String toString() {
//            return "Person{name='" + name + "', age=" + age + "}";
//        }
//    }
//}
