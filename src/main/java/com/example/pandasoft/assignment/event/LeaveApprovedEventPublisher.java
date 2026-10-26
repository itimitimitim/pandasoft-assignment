package com.example.pandasoft.assignment.event;

import com.example.pandasoft.assignment.entity.LeaveRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class LeaveApprovedEventPublisher {

    private final KafkaTemplate<String, LeaveApproved> kafkaTemplate;
    private final String topic;

    public LeaveApprovedEventPublisher(
            KafkaTemplate<String, LeaveApproved> kafkaTemplate,
            @Value("${kafka.topics.leave-approved:leave.approved}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(LeaveRequest request) {
        kafkaTemplate.send(topic, request.getId().toString(), new LeaveApproved(
                request.getId(), request.getEmployeeId(), request.getLeaveType(),
                request.getStartDate().toString(), request.getEndDate().toString(), request.getDays(),
                request.getDecidedBy(), request.getDecidedAt().toString()));
    }
}
