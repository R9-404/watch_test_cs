package com.example.careold.controller;

import com.example.careold.common.ReturnCodeUtil;
import com.example.careold.dao.CustomerDao;
import com.example.careold.domain.Customer;
import org.apache.commons.collections.map.ListOrderedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/restful/customer")
public class CustomerController {

    @Autowired
    CustomerDao customerDao;

    @ResponseBody
    @PostMapping("/select")
    public ListOrderedMap select(@RequestBody ListOrderedMap params){
        System.out.println("进入");
        ListOrderedMap result=new ListOrderedMap();
        String customerName=params.get("customerName").toString();
        List<Customer> customers=customerDao.getCustomer(customerName);
        result.put("customers",customers);
        return result;
    }

    @ResponseBody
    @PostMapping("/add")
    public ListOrderedMap add(@RequestBody ListOrderedMap params){
        ListOrderedMap result=new ListOrderedMap();
        int customerId=customerDao.getMaxId()+1;
        String customerName=params.get("customerName").toString();
        String customerAge=params.get("customerAge").toString();
        String customerSex=params.get("customerSex").toString();
        String customerPhone=params.get("customerPhone").toString();
        String customerAddress=params.get("customerAddress").toString();
        Customer customers=new Customer();
        customers.setCustomerId(customerId);
        customers.setCustomerName(customerName);
        customers.setCustomerAge(Integer.parseInt(customerAge));
        customers.setCustomerSex(customerSex);
        customers.setCustomerPhone(customerPhone);
        customers.setCustomerAddress(customerAddress);
        int row=customerDao.addCustomer(customers);
        if(row==1){
            result.put(ReturnCodeUtil.returnCode,ReturnCodeUtil.successCode);
            return result;
        }
        result.put(ReturnCodeUtil.returnCode,ReturnCodeUtil.falseCode);
        return result;
    }

    @ResponseBody
    @PostMapping("/update")
    public ListOrderedMap update(@RequestBody ListOrderedMap params){
        ListOrderedMap result=new ListOrderedMap();
        String customerId=params.get("customerId").toString();
        String customerName=params.get("customerName").toString();
        String customerAge=params.get("customerAge").toString();
        String customerSex=params.get("customerSex").toString();
        String customerPhone=params.get("customerPhone").toString();
        String customerAddress=params.get("customerAddress").toString();
        Customer customers=new Customer();
        customers.setCustomerId(Integer.parseInt(customerId));
        customers.setCustomerName(customerName);
        customers.setCustomerAge(Integer.parseInt(customerAge));
        customers.setCustomerSex(customerSex);
        customers.setCustomerPhone(customerPhone);
        customers.setCustomerAddress(customerAddress);
        int row=customerDao.updateCustomer(customers);
        if(row==1){
            result.put(ReturnCodeUtil.returnCode,ReturnCodeUtil.successCode);
            return result;
        }
        result.put(ReturnCodeUtil.returnCode,ReturnCodeUtil.falseCode);
        return result;
    }

    @ResponseBody
    @PostMapping("/delete")
    public ListOrderedMap delete(@RequestBody ListOrderedMap params){
        ListOrderedMap result=new ListOrderedMap();
        String customerId=params.get("customerId").toString();
        int row=customerDao.deleteCustomer(Integer.parseInt(customerId));
        if(row==1){
            result.put(ReturnCodeUtil.returnCode,ReturnCodeUtil.successCode);
            return result;
        }
        result.put(ReturnCodeUtil.returnCode,ReturnCodeUtil.falseCode);
        return result;
    }
}
