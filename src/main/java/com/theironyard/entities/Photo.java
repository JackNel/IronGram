package com.theironyard.entities;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Created by Jack on 11/17/15.
 */
@Entity
@Table(name = "photos")
public class Photo {
    @Id
    @GeneratedValue
    @Column(nullable = false)
    public int id;

    @ManyToOne
    public User sender;

    @ManyToOne
    public User receiver;

    @Column(nullable = false)
    public String filename;

    @Column
    public LocalDateTime accessTime;

    @Column(nullable = false)
    public int seconds;

    @Column
    public boolean isPublic;
}
