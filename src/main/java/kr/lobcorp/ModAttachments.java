package kr.lobcorp;

import kr.lobcorp.work.EmployeeData;
import kr.lobcorp.work.FacilityData;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

public final class ModAttachments {
    private ModAttachments() {}

    public static final AttachmentType<EmployeeData> EMPLOYEE = AttachmentRegistry.create(LobCorp.id("employee"),
            b -> b.persistent(EmployeeData.CODEC).copyOnDeath().initializer(EmployeeData::new));

    public static final AttachmentType<FacilityData> FACILITY = AttachmentRegistry.create(LobCorp.id("facility"),
            b -> b.persistent(FacilityData.CODEC).initializer(FacilityData::new));

    public static void init() {}
}
