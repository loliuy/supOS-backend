package com.supos.adpter.nodered.service.enums;

import com.supos.common.annotation.ProtocolIdentifierProvider;
import com.supos.common.dto.protocol.ModbusConfigDTO;
import com.supos.common.dto.protocol.ModbusServerConfigDTO;
import com.supos.common.dto.protocol.ProtocolTagEnums;
import com.supos.common.enums.IOTProtocol;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 枚举modbus位号
 */
@Slf4j
@Service("modbusNodeIdentifiers")
@ProtocolIdentifierProvider(IOTProtocol.MODBUS)
public class ModbusNodeIdentifiers implements IdentifiersInterface<ModbusConfigDTO> {

    @Override
    public List<ProtocolTagEnums> listTags(ModbusConfigDTO config, String topic) {
        int start = Integer.parseInt(config.getAddress());
        int length = Integer.parseInt(config.getQuantity());
        List<ProtocolTagEnums> tags = new ArrayList<>();
        for (int i = start; i < length; i++) {
            if ("Coil".equals(config.getFc()) || "Input".equals(config.getFc())) {
                tags.add(new ProtocolTagEnums(i + "", "boolean"));
            } else {
                tags.add(new ProtocolTagEnums(i + "", "int"));
            }
        }

        return tags;
    }

    /*public static void main(String[] args) {
        // Modbus TCP设备的IP地址和端口
        String ipAddress = "192.168.18.40";
        int port = Modbus.DEFAULT_PORT;

        // 创建Modbus TCP Master
        ModbusTCPMaster master = new ModbusTCPMaster(ipAddress, port);

        try {
            // 连接到Modbus设备
            master.connect();

            // 读取保持寄存器（Holding Registers）
            int slaveId = 1; // 从站ID
            int startAddress = 0; // 起始地址
            int quantity = 10; // 读取的寄存器数量
            Register[] registers = master.readMultipleRegisters(slaveId, startAddress, quantity);

            BitVector bitVector = master.readCoils(slaveId, startAddress, quantity);


            InputRegister[] inputRegisters = master.readInputRegisters(slaveId, startAddress, quantity);

            // 打印读取到的寄存器值
            for (int i = 0; i < registers.length; i++) {

                System.out.println("Register " + (startAddress + i) + ": " + registers[i].getValue());
            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 断开连接
            master.disconnect();
        }
    }*/

}
