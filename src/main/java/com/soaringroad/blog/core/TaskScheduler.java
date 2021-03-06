package com.soaringroad.blog.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.soaringroad.blog.entity.common.Article;
import com.soaringroad.blog.repository.RedisRepository;
import com.soaringroad.blog.repository.SrBlogRepository;
import com.soaringroad.blog.service.NotifyBaiduService;
import com.soaringroad.blog.util.SrBlogConsts;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TaskScheduler {

    @Autowired
    private RedisRepository redisRepository;

    @Autowired
    private SrBlogRepository<Article, Long> repository;
    
    @Autowired
    private NotifyBaiduService notifyBaiduService;

    private static AtomicInteger counter = new AtomicInteger(0);

    @Scheduled(fixedDelay = 3600000)
    public void execute() {
        log.info("定时Task启动");
        Iterable<Article> itr = repository.findAll();
        List<String> urls = new ArrayList<String>();
        urls.add(SrBlogConsts.HOME_URL);
        urls.add(SrBlogConsts.INTRODUCTION_URL);
        Set<String> categories = new HashSet<String>();
        Set<String> labels = new HashSet<String>();
        for (Article article : itr) {
            flushRedisToRdb(article);
            urls.add(String.format(SrBlogConsts.ARTICTLE_URL_TEMPLATE, article.getId()));
            categories.add(article.getCategory());
            labels.addAll(article.getLabels());
        }
        int count = counter.incrementAndGet();
        if (count == 24) {
            notifyBaiduService.notifyBaidu(urls,categories,labels);
            counter.set(0);
        }
        log.info("定时Task结束");
    }

    private void flushRedisToRdb(Article article) {
        Object obj = redisRepository
                .getValue(String.format(SrBlogConsts.REDIS_KEY_ARTICLE_VIEW_COUNT, article.getId()));
        if (obj == null) {
            return;
        }
        article.setView(Long.valueOf(obj.toString()));
        repository.save(article);
    }
}
