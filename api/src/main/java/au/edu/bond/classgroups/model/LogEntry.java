package au.edu.bond.classgroups.model;

import au.edu.bond.classgroups.logging.TaskLogger;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by Shane Argo on 11/06/2014.
 */
@Entity
@Table(name="bond_ClassGroups_log")
public class LogEntry {

    public static enum Level {
        INFO, WARNING, ERROR
    }

    private Long id;
    private Long taskId;
    private Date date;
    private Level level;
    private String message;
    private String stacktrace;


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "bond_classgroups_log_seq")
    @SequenceGenerator(name="bond_classgroups_log_seq", sequenceName = "bond_classgroups_log_seq", allocationSize = 1)
    @Column(name="pk1")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(name="task_pk1")
    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    @Column(name="logged_date")
    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Column(name="log_level")
    @Enumerated(EnumType.STRING)
    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    @Column(name="message")
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Column(name="stacktrace")
    public String getStacktrace() {
        return stacktrace;
    }

    public void setStacktrace(String stacktrace) {
        this.stacktrace = stacktrace;
    }
}
