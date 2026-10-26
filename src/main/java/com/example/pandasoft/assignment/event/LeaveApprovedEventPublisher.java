package com.example.pandasoft.assignment.event;

import com.example.pandasoft.assignment.entity.LeaveRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class LeaveApprovedEventPublisher {

    private final KafkaTemplate<String, LeaveApproved> kafkaTemplate;
    private final String topic;
    private final boolean enabled;

    public LeaveApprovedEventPublisher(
            KafkaTemplate<String, LeaveApproved> kafkaTemplate,
            @Value("${kafka.topics.leave-approved:leave.approved}") String topic,
            @Value("${kafka.enabled:true}") boolean enabled) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.enabled = enabled;
    }

    public void publish(LeaveRequest request) {
        if (!enabled) {
            return;
        }
        kafkaTemplate.send(topic, request.getId().toString(), new LeaveApproved(
                request.getId(), request.getEmployeeId(), request.getLeaveType(),
                request.getStartDate().toString(), request.getEndDate().toString(), request.getDays(),
                request.getDecidedBy(), request.getDecidedAt().toString()));
    }
}
