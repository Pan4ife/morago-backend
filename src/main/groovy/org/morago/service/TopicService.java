package org.morago.service;

import lombok.RequiredArgsConstructor;
import org.morago.dto.topic.TopicRequest;
import org.morago.dto.topic.TopicResponse;
import org.morago.model.Topic;
import org.morago.repository.TopicRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;

    public List<TopicResponse> getAll() {

        return topicRepository.findAll()
                .stream()
                .map(topic ->
                        new TopicResponse(
                                topic.getId(),
                                topic.getName()
                        )
                )
                .toList();

    }

    public TopicResponse create(TopicRequest request) {

        Topic topic = new Topic();

        topic.setName(request.name());

        Topic savedTopic = topicRepository.save(topic);

        return new TopicResponse(savedTopic.getId(), savedTopic.getName());

    }

    public void delete(Long id) {

        topicRepository.deleteById(id);

    }



}
