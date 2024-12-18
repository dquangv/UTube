package controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.servlet.http.HttpSession;

import com.bean.Video;
import com.bean.VideoStatistics;
import com.bean.Watched;
import com.dao.VideoDAO;
import com.dao.WatchedDAO;
import com.bean.Share;
import com.bean.User;

import com.dao.UserDao;

@WebServlet({ "/views/TrangChu", "/views/watched", "/views/thongke", "/views/favorite_video", "/views/details/*",
		"/views/create_update", "/views/update_video", "/views/delete_video", "/views/dangky", "/views/dangnhap",
		"/views/thongtincanhan" })
@MultipartConfig
public class MainPageServlet extends HttpServlet {
	VideoDAO vidDao = new VideoDAO();
	UserDao userDao = new UserDao();

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		resp.setCharacterEncoding("UTF-8");
		String url = req.getRequestURI();
		String viewPath = "";
		String method = req.getMethod();
		System.out.println("URL " + url);
		System.out.println("Method " + method);

		int page = 1;
		int pageSize = 5;

		try {
			page = Integer.parseInt(req.getParameter("page"));
		} catch (NumberFormatException e) {
			page = 1;
		}

		List<Video> allVideos = vidDao.findAll();
		int totalItems = allVideos.size();
		int start = (page - 1) * pageSize;
		int end = Math.min(start + pageSize, totalItems);

		List<Video> paginatedVideos = allVideos.subList(start, end);

		req.setAttribute("listViDeo", paginatedVideos);
		req.setAttribute("currentPage", page);
		req.setAttribute("totalPages", (int) Math.ceil((double) totalItems / pageSize));

		HttpSession session = req.getSession();
		String email = (String) session.getAttribute("user");
		User userAccount = userDao.getUserByRole(true);
		req.setAttribute("user", userAccount);

		if (url.contains("TrangChu")) {
			viewPath = "/views/TrangChu.jsp";
			List<Video> listViDeo = vidDao.findAll();
			if (listViDeo != null && !listViDeo.isEmpty()) {
			} else {
				System.out.println("List is empty or null");
			}

			req.setAttribute("listViDeo", listViDeo);
		} else if (url.contains("watched")) {
			RequestDispatcher dispatcher = req.getRequestDispatcher("/watched_video");
			dispatcher.include(req, resp);
			List<Video> watchedVideo = (List<Video>) req.getAttribute("listVideo");
			req.setAttribute("watched_video", watchedVideo);

			viewPath = "/views/watched.jsp";
		} else if (url.contains("thongke")) {
			RequestDispatcher dispatcher = req.getRequestDispatcher("/VideoStatistics");
			dispatcher.include(req, resp);
			List<Video> videoStatistics = (List<Video>) req.getAttribute("videoStatistics");
			req.setAttribute("videoStatistics", videoStatistics);
			List<VideoStatistics> videoDetailStatistics = (List<VideoStatistics>) req
					.getAttribute("videoDetailStatistics");
			req.setAttribute("videoDetailStatistics", videoDetailStatistics);
//			List<String> distinctVideoTitles = (List<String>) req.getAttribute("distinctVideoTitles");
//			req.setAttribute("distinctVideoTitles", distinctVideoTitles);
//			
//			List<Share> Share = (List<Share>) req.getAttribute("shareList");
//			req.setAttribute("shareVideo", Share);
			viewPath = "/views/thongke.jsp";
		} else if (url.contains("favorite_video")) {
			if (email != null) {
				req.getRequestDispatcher("/favorite_video").forward(req, resp);
				return;
			} else {
				req.getRequestDispatcher("/views/dangnhap.jsp").forward(req, resp);
				return;
			}
		} else if (url.contains("details")) {
			req.setCharacterEncoding("UTF-8");
			resp.setCharacterEncoding("UTF-8");

			String emailDetail = (String) session.getAttribute("user");
			if (emailDetail == null) {
				resp.sendRedirect(req.getContextPath() + "/views/dangnhap.jsp");
				return;
			}

			UserDao userDAO = new UserDao();
			User user = userDAO.getUserByEmail(emailDetail);
			String uri = req.getRequestURI();
			String path = req.getPathInfo();
			int videoId = Integer.parseInt(path.substring(1));
			Integer userId = user.getId();
			Video video = vidDao.findById(videoId);
			WatchedDAO dao = new WatchedDAO();
			Watched watched = null;

			dao.updateOrInsertWatched(userId, videoId);
			try {
				watched = dao.findWatchedByVideoId(emailDetail, videoId);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			if (watched == null) {
				Watched newWatched = new Watched();
				newWatched.setUser(user);
				newWatched.setVideo(video);
				watched = dao.create(newWatched);
			}

			System.out.println("Watched " + watched);

			req.setAttribute("watched", watched);
			List<Video> allActiveVideos = vidDao.findAllActive();
			List<Video> listViDeo = new ArrayList<>();

			for (Video v : allActiveVideos) {
				if (v.getId() != videoId) {
					listViDeo.add(v);
				}
			}

			req.setAttribute("listViDeo", listViDeo);
			req.setAttribute("video", video);
			viewPath = "/views/details.jsp";
		} else if (url.contains("delete_video")) {
			if ("POST".equalsIgnoreCase(method)) {
				handleDelete(req, resp);
			}
			viewPath = "/views/create_update.jsp";
			List<Video> videos = vidDao.findAll();

			List<Video> paginatedVideo = videos.subList(start, end);

			req.setAttribute("listViDeo", paginatedVideo);
			req.setAttribute("currentPage", page);
			req.setAttribute("totalPages", (int) Math.ceil((double) totalItems / pageSize));
		} else if (url.contains("create_update")) {
			if ("POST".equalsIgnoreCase(method)) {
				handleFileUpload(req, resp);
			}
			List<Video> videos = vidDao.findAll();

			List<Video> paginatedVideo = videos.subList(start, end);

			req.setAttribute("listViDeo", paginatedVideo);
			req.setAttribute("currentPage", page);
			req.setAttribute("totalPages", (int) Math.ceil((double) totalItems / pageSize));
			viewPath = "/views/create_update.jsp";
		} else if (url.contains("update_video")) {
			String pageParam = req.getParameter("page");

			if ("GET".equalsIgnoreCase(method) && pageParam == null) {
				handleGetVideo(req, resp);
				viewPath = "/views/update.jsp";
			} else if ("POST".equalsIgnoreCase(method)) {
				handleUpdate(req, resp);
				viewPath = "/views/create_update.jsp";
			} else {
				viewPath = "/views/create_update.jsp";
			}
			List<Video> videos = vidDao.findAll();

			List<Video> paginatedVideo = videos.subList(start, end);

			req.setAttribute("listViDeo", paginatedVideo);
			req.setAttribute("currentPage", page);
			req.setAttribute("totalPages", (int) Math.ceil((double) totalItems / pageSize));

		} else if (url.contains("thongtincanhan")) {
			viewPath = "/views/thongtincanhan.jsp";

			if (email != null) {
				User user = userDao.getUserByEmail(email);
				String imageUrl = user.getImage();
				if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
					req.setAttribute("usesimg", imageUrl);
				} else {
					req.setAttribute("usesimg", "../img/" + imageUrl);
				}
				req.setAttribute("usesd", user.getFullname());
				req.setAttribute("usesa", user.getEmail());
				System.out.println(user.getFullname() + user.getEmail());

				req.setAttribute("view", viewPath);
				req.getRequestDispatcher("/index.jsp").forward(req, resp);
				return;
			} else {
				// forward tới trang đăng nhập nếu không có email trong session
				req.getRequestDispatcher("/views/dangnhap.jsp").forward(req, resp);
				return;
			}
		} else if (url.contains("dangnhap")) {
			req.getRequestDispatcher("/views/dangnhap.jsp").forward(req, resp);
			return;
		} else if (url.contains("dangky")) {
			req.getRequestDispatcher("/views/dangky.jsp").forward(req, resp);
			return;
		}

		req.setAttribute("view", viewPath);
		req.getRequestDispatcher("/index.jsp").forward(req, resp);
	}

	String getYouTubeVideoID(String url) throws Exception {
		URL youtubeUrl = new URL(url);
		String query = youtubeUrl.getQuery();
		String[] params = query.split("&");
		for (String param : params) {
			String[] pair = param.split("=");
			if (pair[0].equals("v")) {
				return pair[1];
			}
		}
		return null;
	}

	private void handleFileUpload(HttpServletRequest request, HttpServletResponse resp)
			throws ServletException, IOException {
		String applicationPath = request.getServletContext().getRealPath("");
		String uploadFilePath = applicationPath + "img";
		File uploadDir = new File(uploadFilePath);
		if (!uploadDir.exists()) {
			uploadDir.mkdirs();
		}

		String fileName = null;
//		Part filePart = request.getPart("photo");
		String videoTitle = request.getParameter("videoTitle");
		String videoLink = request.getParameter("videoLink");
		String imgUrl = videoLink;
		try {
			videoLink = "https://www.youtube.com/embed/" + getYouTubeVideoID(videoLink);
			imgUrl = "https://img.youtube.com/vi/" +  getYouTubeVideoID(imgUrl) + "/0.jpg";

		} catch (Exception ex) {

			System.out.println(ex.getMessage());
		}
		String description = request.getParameter("imageDescription");
		String activateParam = request.getParameter("activationOptions");

		boolean activate = "yes".equals(activateParam);
		System.out.println("upload file " + uploadFilePath + File.separator + fileName);
		Video video = new Video();
		video.setTitle(videoTitle);
		video.setLink(videoLink);
		System.out.println(imgUrl+"IIIIIIIIIMMMMMMMMMMMGGGGGGGGGGGGGG");
		video.setPoster(imgUrl);
		video.setActive(activate);
		video.setDescription(description);
		vidDao.create(video);
//		try {
//			if (filePart != null && filePart.getSubmittedFileName() != null
//					&& !filePart.getSubmittedFileName().isEmpty() && uploadDir.exists()) {
//
//				fileName = filePart.getSubmittedFileName();
//				filePart.write(uploadFilePath + File.separator + fileName);
//
//			
//
//			} else {
//				System.out.println("Video tạo thất bại");
//			}
//		} catch (Exception ex) {
//			System.out.println("Error : " + ex.getMessage());
//		}
	}

	private void handleGetVideo(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		int videoId = 1;
		try {
			videoId = Integer.parseInt(request.getParameter("videoId"));
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		System.out.println("Video id " + videoId);
		Video video = vidDao.findById(videoId);
		if (video != null) {
			request.setAttribute("video", video);
		}
	}

	private void handleDelete(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		int videoId = Integer.parseInt(request.getParameter("videoId"));
		Video video = vidDao.findById(videoId);
		if (video != null) {
			video.setActive(false);
			if (vidDao.update(video)) {
				request.setAttribute("alertMessage", "Xóa video thành công");
			} else {
				request.setAttribute("alertMessage", "Xóa video thất bại");
			}
		}

	}

	private void handleUpdate(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String applicationPath = request.getServletContext().getRealPath("");
		String uploadFilePath = applicationPath + "img";
		File uploadDir = new File(uploadFilePath);
		if (!uploadDir.exists()) {
			uploadDir.mkdirs();
		}

		String fileName = null;
		Part filePart = request.getPart("photo");
		int videoId = Integer.parseInt(request.getParameter("videoId"));

		try {
			if (filePart != null && filePart.getSubmittedFileName() != null
					&& !filePart.getSubmittedFileName().isEmpty() && uploadDir.exists()) {
				fileName = filePart.getSubmittedFileName();
				filePart.write(uploadFilePath + File.separator + fileName);
			}

			String videoTitle = request.getParameter("videoTitle");
			String videoLink = request.getParameter("videoLink");
			String description = request.getParameter("imageDescription");
			String activateParam = request.getParameter("activationOptions");

			boolean activate = "yes".equals(activateParam);

			Video video = vidDao.findById(videoId);
			if (video != null) {
				video.setTitle(videoTitle);
				video.setLink(videoLink);
				if (fileName != null) {
					video.setPoster(fileName);
				}
				video.setActive(activate);
				video.setDescription(description);
				if (vidDao.update(video)) {
					request.setAttribute("alertMessage", "Cập nhật thành công");
				} else {
					request.setAttribute("alertMessage", "Cập nhật thất bại");
				}

			}
		} catch (Exception ex) {
			System.out.println("Error: " + ex.getMessage());
		}
	}
}
