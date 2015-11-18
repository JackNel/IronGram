package com.theironyard.IronGramController;

import com.theironyard.entities.Photo;
import com.theironyard.entities.User;
import com.theironyard.services.PhotoRepository;
import com.theironyard.services.UserRepository;
import com.theironyard.utils.PasswordHash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

/**
 * Created by Jack on 11/17/15.
 */

@RestController
public class IronGramController {
    @Autowired
    UserRepository users;

    @Autowired
    PhotoRepository photos;

    @RequestMapping("/login")
    public User login(HttpSession session, HttpServletResponse response, String username, String password) throws Exception {
        User user = users.findOneByUsername(username);

        if (user == null) {
            user = new User();
            user.username = username;
            user.password = PasswordHash.createHash(password);
            users.save(user);
        }
        else if (!PasswordHash.validatePassword(password, user.password)) {
            throw new Exception("Wrong password");
        }
        session.setAttribute("username", username);
        response.sendRedirect("/");

        return user;
    }

    @RequestMapping("/logout")
    public void logout(HttpSession session, HttpServletResponse response) throws IOException {
        session.invalidate();
        response.sendRedirect("/");
    }

    @RequestMapping("/user")
    public User user(HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return null;
        }
        return users.findOneByUsername(username);
    }

    @RequestMapping("/upload")
    public Photo upload(
            HttpSession session,
            HttpServletResponse response,
            String receiver,
            MultipartFile photo,
            @RequestParam(defaultValue = "10") int seconds,
            boolean isPublic
    ) throws Exception {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            throw new Exception("Not logged in");
        }
        User senderUser = users.findOneByUsername(username);
        User receiverUser = users.findOneByUsername(receiver);
        if (receiverUser == null) {
            throw new Exception("Receiver name doesn't exist.");
        }

        if (!photo.getContentType().startsWith("image")) {
            throw new Exception("Only images are allowed");
        }

        File photoFile = File.createTempFile("photo", photo.getOriginalFilename(), new File("public"));
        FileOutputStream fos = new FileOutputStream(photoFile);
        fos.write(photo.getBytes());

        Photo p = new Photo();
        p.sender = senderUser;
        p.receiver = receiverUser;
        p.filename = photoFile.getName();
        p.seconds = seconds;
        p.isPublic = isPublic;
        photos.save(p);

        response.sendRedirect("/");
        return p;
    }

    @RequestMapping("/photos")
    public List<Photo> showPhotos(HttpSession session) throws Exception {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            throw new Exception("Not logged in");
        }

        User user = users.findOneByUsername(username);
        List<Photo> photoList = photos.findByReceiver(user);
        for (Photo p : photoList) {
            if (p.accessTime == null) {
                p.accessTime = LocalDateTime.now();
                photos.save(p);
              //  waitToDelete(p, p.seconds);
            }
            // All of this commented out stuff are additional ways to delete the file

            else if (p.accessTime.isBefore(LocalDateTime.now().minusSeconds(p.seconds))) {
                photos.delete(p);
                File file = new File("public", p.filename);
                file.delete();
            }

        }
        return photos.findByReceiver(user);
    }

    /*
    public void waitToDelete(Photo photo, int seconds) {

        Thread t = new Thread(() -> {
            try {
                Thread.sleep(seconds * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            photos.delete(photo);
            File f = new File("public", photo.filename);
            f.delete();
        });
        t.start();

        Below is another example using a thread

        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                photos.delete(photo);
                File f = new File("public", photo.filename);
                f.delete();
            }
        }, seconds * 1000);
    }
    */

    @RequestMapping("/public-photos")
    public List<Photo> publicPhotos(String username) throws Exception {
        User user = users.findOneByUsername(username);

        List<Photo> photoList = photos.findBySender(user).stream()
                .filter(p1 -> p1.isPublic)
                .collect(Collectors.toList());
        return photoList;
    }
}
