package com.enterprise.asset.business.config;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@Configuration
public class JacksonConfig {

  @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
  @JsonIgnoreProperties({ "pageable", "sort" })
  public abstract class PageImplMixin<T> {

    @JsonGetter("content")
    abstract java.util.List<T> getContent();

    @JsonGetter("totalElements")
    abstract long getTotalElements();

    @JsonGetter("totalPages")
    abstract int getTotalPages();

    @JsonGetter("number")
    abstract int getNumber();

    @JsonGetter("size")
    abstract int getSize();

    @JsonGetter("numberOfElements")
    abstract int getNumberOfElements();

    @JsonGetter("first")
    abstract boolean isFirst();

    @JsonGetter("last")
    abstract boolean isLast();

    @JsonGetter("empty")
    abstract boolean isEmpty();
  }

  @Bean
  public BeanPostProcessor objectMapperPostProcessor() {
    return new BeanPostProcessor() {
      @Override
      public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ObjectMapper) {
          ObjectMapper mapper = (ObjectMapper) bean;
          mapper.addMixIn(PageImpl.class, PageImplMixin.class);
          mapper.addMixIn(Page.class, PageImplMixin.class);
        }
        return bean;
      }
    };
  }
}