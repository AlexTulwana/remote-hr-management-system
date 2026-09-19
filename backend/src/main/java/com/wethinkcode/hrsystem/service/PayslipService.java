package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.Payslip;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.PayslipRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Value;



import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PayslipService {

    private final PayslipRepository payslipRepository;
    private final EmployeeRepository employeeRepository;
    private final String uploadDir;
    private final CurrentUserService currentUserService;
    private final JavaMailSender mailSender;


    public PayslipService(PayslipRepository payslipRepository,
                          EmployeeRepository employeeRepository,
                          CurrentUserService currentUserService,
                          JavaMailSender mailSender,
                          @Value("${payslip.upload-dir:uploads/payslips/}") String uploadDir) {
        this.payslipRepository = payslipRepository;
        this.employeeRepository = employeeRepository;
        this.currentUserService = currentUserService;
        this.mailSender = mailSender;
        this.uploadDir = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
    }

    public Payslip upload(Long employeeId, String payPeriod, MultipartFile file) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Payslip payslip = new Payslip();
        payslip.setEmployee(employee);
        payslip.setPayPeriod(payPeriod);
        payslip.setUploadedDate(LocalDate.now());
        payslip.setFilePath(saveFile(file));

        return payslipRepository.save(payslip);
    }

    public List<Payslip> getByEmployee(Long employeeId) {
        String role = currentUserService.getCurrentUser().getRole();
        if (!role.equals("HR") && !role.equals("ADMIN")) {
            if (!currentUserService.isSelf(employeeId)) {
                throw new AccessDeniedException("You are not authorized to view these payslips");
            }
        }
        return payslipRepository.findByEmployeeId(employeeId);
    }

    public Payslip getById(Long id) {
        Payslip payslip = payslipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payslip not found"));

        String role = currentUserService.getCurrentUser().getRole();
        if (!role.equals("HR") && !role.equals("ADMIN")) {
            if (!currentUserService.isSelf(payslip.getEmployee().getId())) {
                throw new AccessDeniedException("You are not authorized to view this payslip");
            }
        }

        return payslip;
    }

    public Resource downloadFile(Long payslipId) {
        Payslip payslip = getById(payslipId);
        try {
            Path filePath = Paths.get(payslip.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found: " + payslip.getFilePath());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error loading file: " + e.getMessage());
        }
    }

    public void emailToSelf(Long payslipId) {
        Payslip payslip = getById(payslipId);
        Employee employee = payslip.getEmployee();

        if (employee.getEmail() == null || employee.getEmail().isBlank()) {
            throw new RuntimeException("No email address on file for this employee");
        }

        Resource file = downloadFile(payslipId);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(employee.getEmail());
            helper.setSubject("Your payslip - " + payslip.getPayPeriod());
            helper.setText("Hi " + employee.getFullName() + ",\n\n"
                    + "Please find attached your payslip for " + payslip.getPayPeriod() + ".\n\n"
                    + "Regards,\nHR Team");
            helper.addAttachment("payslip-" + payslip.getPayPeriod() + ".pdf", file);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send payslip email: " + e.getMessage(), e);
        }
    }

    private String saveFile(MultipartFile file) {
        try {
            Files.createDirectories(Paths.get(uploadDir));
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir + filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store payslip file", e);
        }
    }
}
