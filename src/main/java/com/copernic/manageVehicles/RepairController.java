/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.copernic.manageVehicles;

import com.copernic.manageVehicles.domain.Repair;
import com.copernic.manageVehicles.domain.Task;
import com.copernic.manageVehicles.domain.User;
import com.copernic.manageVehicles.domain.Vehicle;
import com.copernic.manageVehicles.services.RepairService;
import com.copernic.manageVehicles.services.TaskService;
import com.copernic.manageVehicles.services.UserServiceImpl;
import com.copernic.manageVehicles.services.VehicleService;
import jakarta.validation.Valid;
import java.util.stream.Collectors;
import java.security.Principal;
import java.util.Collection;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.List;
import java.util.ArrayList;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author enricledo
 */
@Controller
public class RepairController {

    @Autowired
    private RepairService repairService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private VehicleService vehicleService;
    
    @Autowired
    private UserServiceImpl userService;
        
    //Create Repair
    @GetMapping("/repair-form/{numberPlate}")
    public String getEmptyForm(@PathVariable String numberPlate, Model model) {
        Repair repair = new Repair();
        List<Task> tasks = taskService.getAllTasks();
        model.addAttribute("tasks", tasks);

        if (numberPlate != null) {
            Vehicle vehicle = vehicleService.findByNumberPlate(numberPlate);
            if (vehicle != null) {
                repair.setVehicle(vehicle);
                model.addAttribute("numberPlate", numberPlate);
            } else {
                return "redirect:/repairs";// o return "error-page";
            }
        }

        model.addAttribute("repair", repair);
        return "repair-form";
    }

    
    
    //Edit-Update a repair
    @GetMapping("/repairs/edit/{id}")
    public String editRepair(Model model, @PathVariable Long id) {
        Repair repair = repairService.findById(id);
        List<Task> allTasks = taskService.getAllTasks();
        
        List<Long> tasksFromRepair = repair.getTasks().stream()
                                     .map(Task::getId)
                                     .collect(Collectors.toList());
        
        model.addAttribute("repair", repair);
        model.addAttribute("tasksFromRepair", tasksFromRepair);
        model.addAttribute("tasks", allTasks);
        
        return "repair-edit";
    }
    
    //Save a repair
    @PostMapping("/repairs")
    public String saveRepair(@Valid Repair repair, BindingResult result, Model model) {       
        if(result.hasErrors()){
            List<Task> allTasks = taskService.getAllTasks();
            List<Long> tasksFromRepair = repair.getTasks().stream()
                                             .map(Task::getId)
                                             .collect(Collectors.toList());

            model.addAttribute("repair", repair);
            model.addAttribute("tasksFromRepair", tasksFromRepair);
            model.addAttribute("tasks", allTasks);

            if (repair.getRepairId() != null) {
                return "repair-edit";
            } else {
                return "repair-form";
            }
        }
        else{
            repairService.saveRepair(repair);
            return "redirect:/repairs";
        }
    }

    //List of repairs
    @GetMapping("/repairs")
    public String findAll(@RequestParam(required = false) String query, Model model) {
        List<Repair> repairs;
        if (query != null && !query.isEmpty()) {
            repairs = repairService.searchBar(query);
        } else {
            repairs = repairService.getAllRepairs();
        }
        model.addAttribute("repairs", repairs);
        return "repair-list";
    }
    
    //Visualize individual repair    
    @GetMapping("/repairs/view/{id}")
    public String findById(Model model, @PathVariable Long id, Principal principal) {
    Collection<? extends GrantedAuthority> authorities = ((UserDetails) ((Authentication) principal).getPrincipal()).getAuthorities();
    boolean isUser = authorities.contains(new SimpleGrantedAuthority("ROLE_USUARIO"));
    Repair repair = repairService.findById(id);
    UserDetails userDetails = (UserDetails) ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
    Optional<User> userOptional = userService.findByNif(userDetails.getUsername());
    if (repair != null && userOptional.isPresent()) {
        User user = userOptional.get();
        // Comprobar si el usuario logueado es el dueño del vehículo
        if (isUser && !user.getNif().equals(repair.getVehicle().getOwner().getNif())) {
            return "redirect:/error";
        }
        model.addAttribute("repair", repair);
        model.addAttribute("total", repairService.getTotalPrice(id));
        model.addAttribute("user", user); // Agregar el usuario al modelo
        model.addAttribute("numberPlate", repair.getVehicle().getNumberPlate()); // Agregar numberPlate al modelo        
        // Agregar roles al modelo
        addRolesToModel(model, principal);

        return "repair-view";
    } else {
        return "redirect:/error";
    }
}
    //SHOW USER'S VEHICLES
    @GetMapping("/repairs/viewByNumberPlate/{numberPlate}")
    public String showForm(@PathVariable("numberPlate") Vehicle vehicle, Model model) {
        Repair repair = new Repair();
        Vehicle vehicles = vehicleService.findByNumberPlate(vehicle.getNumberPlate());
        repair.setVehicle(vehicles);
        model.addAttribute("repair", repair);
        return "repair-form";
    }
    // Delete a repair
    @GetMapping("/repairs/delete/{id}")
    public String deleteRepair(@PathVariable Long id) {
        repairService.deleteById(id);
        return "redirect:/repairs";
    }
    
    private void addRolesToModel(Model model, Principal principal) {
        Collection<? extends GrantedAuthority> authorities = ((UserDetails) ((UsernamePasswordAuthenticationToken) principal).getPrincipal()).getAuthorities();
        boolean isAdmin = authorities.contains(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"));
        boolean isUser = authorities.contains(new SimpleGrantedAuthority("ROLE_USUARIO"));
        boolean isMecanico = authorities.contains(new SimpleGrantedAuthority("ROLE_MECANICO"));

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isUser", isUser);
        model.addAttribute("isMecanico", isMecanico);
    }   
}
