package au.edu.bond.classgroups.model;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by Shane Argo on 2/06/2014.
 */
@Entity
@Table(name="bond_ClassGroups_task")
public class Task {

    public static enum Status {
        PENDING, PROCESSING, SKIPPED, COMPLETE, FAILED, UNKNOWN, SCHEDULED, CANCELLED, NEW
    }

    private Long id;
    private Status status = Status.UNKNOWN;
    private Date enteredDate;
    private Date scheduledDate;
    private Date startedDate;
    private Date endedDate;
    private String enteredNode;
    private String processingNode;
    private Integer totalGroups;
    private Integer processedGroups;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "bond_classgroups_task_seq")
    @SequenceGenerator(name="bond_classgroups_task_seq", sequenceName = "bond_classgroups_task_seq", allocationSize = 1)
    @Column(name="pk1")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(name="status")
    @Enumerated(EnumType.STRING)
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Column(name="entered_date")
    public Date getEnteredDate() {
        return enteredDate;
    }

    public void setEnteredDate(Date enteredDate) {
        this.enteredDate = enteredDate;
    }

    @Column(name="scheduled_date")
    public Date getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(Date scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    @Column(name="started_date")
    public Date getStartedDate() {
        return startedDate;
    }

    public void setStartedDate(Date startedDate) {
        this.startedDate = startedDate;
    }

    @Column(name="ended_date")
    public Date getEndedDate() {
        return endedDate;
    }

    public void setEndedDate(Date endedDate) {
        this.endedDate = endedDate;
    }

    @Column(name="entered_node")
    public String getEnteredNode() {
        return enteredNode;
    }

    public void setEnteredNode(String enteredNode) {
        this.enteredNode = enteredNode;
    }

    @Column(name="processing_node")
    public String getProcessingNode() {
        return processingNode;
    }

    public void setProcessingNode(String processingNode) {
        this.processingNode = processingNode;
    }

    @Column(name="total_groups")
    public Integer getTotalGroups() {
        return totalGroups;
    }

    public void setTotalGroups(Integer totalGroups) {
        this.totalGroups = totalGroups;
    }

    @Column(name="processed_groups")
    public Integer getProcessedGroups() {
        return processedGroups;
    }

    public void setProcessedGroups(Integer processedGroups) {
        this.processedGroups = processedGroups;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Task) {
            return this.getId().equals(((Task) obj).getId());
        }
        return super.equals(obj);
    }
}
