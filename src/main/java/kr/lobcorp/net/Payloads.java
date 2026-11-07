package kr.lobcorp.net;

import kr.lobcorp.LobCorp;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class Payloads {
    private Payloads() {}

    /** 클라 → 서버: 작업 화면 버튼. action 0~3 = 작업, 11 = 무기 추출, 12 = 방어구 추출 */
    public record WorkAction(int entityId, int action) implements CustomPacketPayload {
        public static final Type<WorkAction> TYPE = new Type<>(LobCorp.id("work_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WorkAction> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, WorkAction::entityId,
                ByteBufCodecs.VAR_INT, WorkAction::action,
                WorkAction::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 서버 → 클라: 작업 화면 열기 */
    public record OpenWork(int entityId, String abnoId, int pe, int qliphoth, boolean busy) implements CustomPacketPayload {
        public static final Type<OpenWork> TYPE = new Type<>(LobCorp.id("open_work"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenWork> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, OpenWork::entityId,
                ByteBufCodecs.STRING_UTF8, OpenWork::abnoId,
                ByteBufCodecs.VAR_INT, OpenWork::pe,
                ByteBufCodecs.VAR_INT, OpenWork::qliphoth,
                ByteBufCodecs.BOOL, OpenWork::busy,
                OpenWork::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * 서버 → 클라: 직원 상태 동기화 (HUD용)
     * 0 sp, 1 spMax, 2 용기, 3 신중, 4 자제, 5 정의, 6 패닉틱,
     * 7 작업중(0/1), 8 박스, 9 최대박스, 10 성공, 11 작업종류, 12 요정 축복 틱, 13 수면 틱, 14 홀림 틱, 15 라티샤의 선물(0/1),
     * 16 대피소 안(0/1), 17 초상(0=없음, 1=새겨진 자, 2=대신하는 자), 18 노트 보유 초(0=없음),
     * 19 우정의 조약돌(0/1), 20 눈의 여왕의 저주(0=없음, 1=얼음 조각, 2=얼어붙음)
     */
    public record EmployeeSync(int[] v) implements CustomPacketPayload {
        public static final int SIZE = 21;
        public static final Type<EmployeeSync> TYPE = new Type<>(LobCorp.id("employee_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, EmployeeSync> CODEC = StreamCodec.of(
                (buf, p) -> {
                    for (int i = 0; i < SIZE; i++) buf.writeVarInt(p.v[i]);
                },
                buf -> {
                    int[] v = new int[SIZE];
                    for (int i = 0; i < SIZE; i++) v[i] = buf.readVarInt();
                    return new EmployeeSync(v);
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(WorkAction.TYPE, WorkAction.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenWork.TYPE, OpenWork.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EmployeeSync.TYPE, EmployeeSync.CODEC);
    }
}
