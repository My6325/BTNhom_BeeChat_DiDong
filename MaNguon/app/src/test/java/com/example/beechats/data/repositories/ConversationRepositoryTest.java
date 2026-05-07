package com.example.beechats.data.repositories;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Unit tests cho ConversationRepository.
 * Mock Firestore chain để test không cần device/emulator.
 */
public class ConversationRepositoryTest {

    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private CollectionReference mockCollection;

    @Mock
    private DocumentReference mockDocument;

    // Mocks cho createGroupConversation (WriteBatch + subcollection members/)
    @Mock
    private WriteBatch mockBatch;

    @Mock
    private CollectionReference mockMembersCollection;

    @Mock
    private DocumentReference mockMemberRef;

    // Mock cho removeMember / setMemberRole (role check via DocumentSnapshot)
    @Mock
    private DocumentSnapshot mockMemberSnapshot;

    // Mock cho setMemberRole (kiểm tra adminIds trong conversation doc)
    @Mock
    private DocumentSnapshot mockConvSnapshot;

    @Mock
    private ConversationRepository.OnConversationCallback mockCallback;

    @Mock
    private ConversationRepository.OnCompleteCallback mockCompleteCallback;

    private ConversationRepository repository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Stub Firestore chain dùng chung cho private conversation
        when(mockFirestore.collection(anyString())).thenReturn(mockCollection);
        when(mockCollection.document(anyString())).thenReturn(mockDocument);
        // Stub no-arg document() cho createGroupConversation (random ID)
        when(mockCollection.document()).thenReturn(mockDocument);
        // Stub mockDocument.getId() → trả về ID cố định để test
        when(mockDocument.getId()).thenReturn("mockGroupConvId");
        // Stub subcollection members/ bên trong conversation doc
        when(mockDocument.collection("members")).thenReturn(mockMembersCollection);
        when(mockMembersCollection.document(anyString())).thenReturn(mockMemberRef);
        // Stub WriteBatch
        when(mockFirestore.batch()).thenReturn(mockBatch);
        when(mockBatch.set(any(DocumentReference.class), anyMap())).thenReturn(mockBatch);
        when(mockBatch.update(any(DocumentReference.class), anyMap())).thenReturn(mockBatch);
        when(mockBatch.delete(any(DocumentReference.class))).thenReturn(mockBatch);
        repository = new ConversationRepository(mockFirestore);
    }

    // -----------------------------------------------------------------------
    // TC26: Happy path — uid hợp lệ → set() được gọi → onSuccess(conversationId)
    // -----------------------------------------------------------------------

    @Test
    public void createOrGetPrivateConversation_validUids_callsSetAndOnSuccess() {
        Task<Void> setTask = buildVoidSuccessTask();
        when(mockDocument.set(anyMap(), any(SetOptions.class))).thenReturn(setTask);

        repository.createOrGetPrivateConversation("aaaa", "bbbb", mockCallback);

        verify(mockDocument).set(anyMap(), any(SetOptions.class));
        verify(mockCallback).onSuccess("aaaa_bbbb");
        verify(mockCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC27: Deterministic ID — đảo thứ tự uid vẫn sinh cùng conversationId
    // -----------------------------------------------------------------------

    @Test
    public void createOrGetPrivateConversation_reversedUids_sameConversationId() {
        Task<Void> setTask = buildVoidSuccessTask();
        when(mockDocument.set(anyMap(), any(SetOptions.class))).thenReturn(setTask);

        // Đảo ngược thứ tự: bbbb trước aaaa
        repository.createOrGetPrivateConversation("bbbb", "aaaa", mockCallback);

        // conversationId vẫn phải là "aaaa_bbbb" (sorted)
        verify(mockCallback).onSuccess("aaaa_bbbb");
        verify(mockCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC28: uid1 null hoặc rỗng → onError ngay, Firestore không gọi
    // -----------------------------------------------------------------------

    @Test
    public void createOrGetPrivateConversation_nullUid1_callsOnErrorWithoutFirestore() {
        repository.createOrGetPrivateConversation(null, "bbbb", mockCallback);

        verify(mockCallback).onError("ID người dùng không hợp lệ.");
        verify(mockCallback, never()).onSuccess(anyString());
        verify(mockDocument, never()).set(anyMap(), any(SetOptions.class));
    }

    @Test
    public void createOrGetPrivateConversation_emptyUid1_callsOnErrorWithoutFirestore() {
        repository.createOrGetPrivateConversation("", "bbbb", mockCallback);

        verify(mockCallback).onError("ID người dùng không hợp lệ.");
        verify(mockCallback, never()).onSuccess(anyString());
        verify(mockDocument, never()).set(anyMap(), any(SetOptions.class));
    }

    // -----------------------------------------------------------------------
    // TC29: uid2 null hoặc rỗng → onError ngay, Firestore không gọi
    // -----------------------------------------------------------------------

    @Test
    public void createOrGetPrivateConversation_nullUid2_callsOnErrorWithoutFirestore() {
        repository.createOrGetPrivateConversation("aaaa", null, mockCallback);

        verify(mockCallback).onError("ID người dùng không hợp lệ.");
        verify(mockCallback, never()).onSuccess(anyString());
        verify(mockDocument, never()).set(anyMap(), any(SetOptions.class));
    }

    // -----------------------------------------------------------------------
    // TC30: Firestore set() fail → onError(message), onSuccess() không gọi
    // -----------------------------------------------------------------------

    @Test
    public void createOrGetPrivateConversation_firestoreFails_callsOnErrorWithMessage() {
        Exception exception = new Exception("Firestore network error");
        Task<Void> setTask = buildVoidFailureTask(exception);
        when(mockDocument.set(anyMap(), any(SetOptions.class))).thenReturn(setTask);

        repository.createOrGetPrivateConversation("aaaa", "bbbb", mockCallback);

        verify(mockCallback).onError("Firestore network error");
        verify(mockCallback, never()).onSuccess(anyString());
    }

    // -----------------------------------------------------------------------
    // Helper: tạo mock Task<Void> thành công
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Task<Void> buildVoidSuccessTask() {
        Task<Void> mockTask = mock(Task.class);
        doAnswer(inv -> {
            ((OnSuccessListener<Void>) inv.getArgument(0)).onSuccess(null);
            return mockTask;
        }).when(mockTask).addOnSuccessListener(any(OnSuccessListener.class));
        when(mockTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockTask);
        return mockTask;
    }

    // -----------------------------------------------------------------------
    // Helper: tạo mock Task<Void> thất bại
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Task<Void> buildVoidFailureTask(Exception exception) {
        Task<Void> mockTask = mock(Task.class);
        when(mockTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(mockTask);
        doAnswer(inv -> {
            ((OnFailureListener) inv.getArgument(0)).onFailure(exception);
            return mockTask;
        }).when(mockTask).addOnFailureListener(any(OnFailureListener.class));
        return mockTask;
    }

    // -----------------------------------------------------------------------
    // TC90: creatorId null → onError ngay, Firestore không gọi
    // -----------------------------------------------------------------------

    @Test
    public void createGroupConversation_nullCreatorId_callsOnError() {
        repository.createGroupConversation(null, "Nhóm A",
                Arrays.asList("uid1", "uid2", "uid3"), mockCallback);

        verify(mockCallback).onError("ID người tạo không hợp lệ.");
        verify(mockCallback, never()).onSuccess(anyString());
        verify(mockFirestore, never()).batch();
    }

    // -----------------------------------------------------------------------
    // TC91: groupName null → onError ngay; TC91b: groupName rỗng → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void createGroupConversation_nullGroupName_callsOnError() {
        repository.createGroupConversation("uid1", null,
                Arrays.asList("uid1", "uid2", "uid3"), mockCallback);

        verify(mockCallback).onError("Tên nhóm không được để trống.");
        verify(mockCallback, never()).onSuccess(anyString());
        verify(mockFirestore, never()).batch();
    }

    @Test
    public void createGroupConversation_emptyGroupName_callsOnError() {
        repository.createGroupConversation("uid1", "  ",
                Arrays.asList("uid1", "uid2", "uid3"), mockCallback);

        verify(mockCallback).onError("Tên nhóm không được để trống.");
        verify(mockCallback, never()).onSuccess(anyString());
    }

    // -----------------------------------------------------------------------
    // TC92: memberIds null → onError ngay; TC92b: memberIds rỗng → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void createGroupConversation_nullMemberIds_callsOnError() {
        repository.createGroupConversation("uid1", "Nhóm A", null, mockCallback);

        verify(mockCallback).onError("Danh sách thành viên không hợp lệ.");
        verify(mockCallback, never()).onSuccess(anyString());
        verify(mockFirestore, never()).batch();
    }

    @Test
    public void createGroupConversation_emptyMemberIds_callsOnError() {
        repository.createGroupConversation("uid1", "Nhóm A",
                Arrays.asList(), mockCallback);

        verify(mockCallback).onError("Danh sách thành viên không hợp lệ.");
        verify(mockCallback, never()).onSuccess(anyString());
    }

    // -----------------------------------------------------------------------
    // TC93: Happy path — 3 members (creator trong list) → batch.commit() gọi,
    //        onSuccess("mockGroupConvId"), batch.set() gọi 4 lần (1 conv + 3 member)
    // -----------------------------------------------------------------------

    @Test
    public void createGroupConversation_validInputs_batchCommitAndOnSuccess() {
        Task<Void> commitTask = buildVoidSuccessTask();
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.createGroupConversation("uid1", "Nhóm A",
                Arrays.asList("uid1", "uid2", "uid3"), mockCallback);

        // batch.set() gọi 4 lần: 1 conversation doc + 3 member docs
        verify(mockBatch, times(4)).set(any(DocumentReference.class), anyMap());
        verify(mockBatch).commit();
        verify(mockCallback).onSuccess("mockGroupConvId");
        verify(mockCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC94: Creator chưa có trong memberIds → tự thêm vào, participants.size() = 3
    // -----------------------------------------------------------------------

    @Test
    public void createGroupConversation_creatorNotInMembers_creatorAddedToParticipants() {
        Task<Void> commitTask = buildVoidSuccessTask();
        when(mockBatch.commit()).thenReturn(commitTask);

        // memberIds chỉ có uid2, uid3 — creator uid1 chưa có
        repository.createGroupConversation("uid1", "Nhóm B",
                Arrays.asList("uid2", "uid3"), mockCallback);

        // batch.set() gọi 3 lần: 1 conversation + 2 member + 1 creator = 4
        verify(mockBatch, times(4)).set(any(DocumentReference.class), anyMap());
        // Verify subcollection members/ được truy cập 3 lần (uid2, uid3, uid1)
        verify(mockMembersCollection, times(3)).document(anyString());
        verify(mockCallback).onSuccess("mockGroupConvId");
    }

    // -----------------------------------------------------------------------
    // TC95: Creator có role="admin" trong member document
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    public void createGroupConversation_creatorHasAdminRole_inMemberDoc() {
        Task<Void> commitTask = buildVoidSuccessTask();
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.createGroupConversation("uid1", "Nhóm A",
                Arrays.asList("uid1", "uid2", "uid3"), mockCallback);

        // Capture tất cả Map args truyền vào batch.set()
        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockBatch, times(4)).set(any(DocumentReference.class), mapCaptor.capture());

        // Map đầu tiên là convData, từ map thứ 2 trở đi là member docs
        List<Map<String, Object>> allMaps = mapCaptor.getAllValues();

        // Tìm member doc có memberId = "uid1" — phải có role="admin"
        boolean foundAdminRole = allMaps.stream()
                .filter(m -> "uid1".equals(m.get("memberId")))
                .anyMatch(m -> "admin".equals(m.get("role")));
        assertTrue("Creator uid1 phải có role=admin", foundAdminRole);
    }

    // -----------------------------------------------------------------------
    // TC96: Non-creator có role="member" trong member document
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    public void createGroupConversation_nonCreatorHasMemberRole_inMemberDoc() {
        Task<Void> commitTask = buildVoidSuccessTask();
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.createGroupConversation("uid1", "Nhóm A",
                Arrays.asList("uid1", "uid2", "uid3"), mockCallback);

        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockBatch, times(4)).set(any(DocumentReference.class), mapCaptor.capture());

        List<Map<String, Object>> allMaps = mapCaptor.getAllValues();

        // Verify uid2 và uid3 có role="member"
        boolean uid2IsMember = allMaps.stream()
                .filter(m -> "uid2".equals(m.get("memberId")))
                .anyMatch(m -> "member".equals(m.get("role")));
        boolean uid3IsMember = allMaps.stream()
                .filter(m -> "uid3".equals(m.get("memberId")))
                .anyMatch(m -> "member".equals(m.get("role")));
        assertTrue("uid2 phải có role=member", uid2IsMember);
        assertTrue("uid3 phải có role=member", uid3IsMember);
    }

    // -----------------------------------------------------------------------
    // TC97: convData có participantCount = 3 khi 3 members đều là uid hợp lệ
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    public void createGroupConversation_participantCountMatchesMemberCount() {
        Task<Void> commitTask = buildVoidSuccessTask();
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.createGroupConversation("uid1", "Nhóm A",
                Arrays.asList("uid1", "uid2", "uid3"), mockCallback);

        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockBatch, atLeastOnce()).set(eq(mockDocument), mapCaptor.capture());

        // Map được capture từ convRef (mockDocument) là convData
        Map<String, Object> convData = mapCaptor.getValue();
        assertTrue("participantCount phải là 3",
                Integer.valueOf(3).equals(convData.get("participantCount")));
    }

    // -----------------------------------------------------------------------
    // TC98: convData có adminIds chứa creatorId
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    public void createGroupConversation_adminIdsContainsCreator() {
        Task<Void> commitTask = buildVoidSuccessTask();
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.createGroupConversation("uid1", "Nhóm A",
                Arrays.asList("uid1", "uid2", "uid3"), mockCallback);

        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockBatch, atLeastOnce()).set(eq(mockDocument), mapCaptor.capture());

        Map<String, Object> convData = mapCaptor.getValue();
        List<?> adminIds = (List<?>) convData.get("adminIds");
        assertTrue("adminIds phải chứa creator uid1",
                adminIds != null && adminIds.contains("uid1"));
    }

    // -----------------------------------------------------------------------
    // TC99: batch.commit() fail → onError(message), onSuccess() không gọi
    // -----------------------------------------------------------------------

    @Test
    public void createGroupConversation_batchCommitFails_callsOnError() {
        Exception exception = new Exception("Batch commit error");
        Task<Void> failTask = buildVoidFailureTask(exception);
        when(mockBatch.commit()).thenReturn(failTask);

        repository.createGroupConversation("uid1", "Nhóm A",
                Arrays.asList("uid1", "uid2", "uid3"), mockCallback);

        verify(mockCallback).onError("Batch commit error");
        verify(mockCallback, never()).onSuccess(anyString());
    }

    // -----------------------------------------------------------------------
    // TC100: addMember — convId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void addMember_nullConvId_callsOnError() {
        repository.addMember(null, "uid2", mockCompleteCallback);

        verify(mockCompleteCallback).onError("ID hội thoại không hợp lệ.");
        verify(mockCompleteCallback, never()).onSuccess();
        verify(mockFirestore, never()).batch();
    }

    // -----------------------------------------------------------------------
    // TC101: addMember — userId null/rỗng → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void addMember_nullUserId_callsOnError() {
        repository.addMember("conv1", null, mockCompleteCallback);

        verify(mockCompleteCallback).onError("ID thành viên không hợp lệ.");
        verify(mockCompleteCallback, never()).onSuccess();
    }

    @Test
    public void addMember_emptyUserId_callsOnError() {
        repository.addMember("conv1", "  ", mockCompleteCallback);

        verify(mockCompleteCallback).onError("ID thành viên không hợp lệ.");
        verify(mockCompleteCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC102: addMember — happy path → batch.update + batch.set + onSuccess()
    // -----------------------------------------------------------------------

    @Test
    public void addMember_validInputs_batchUpdateAndSetAndOnSuccess() {
        Task<Void> commitTask = buildVoidSuccessTask();
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.addMember("conv1", "uid2", mockCompleteCallback);

        verify(mockBatch).update(eq(mockDocument), anyMap());
        verify(mockBatch).set(eq(mockMemberRef), anyMap());
        verify(mockBatch).commit();
        verify(mockCompleteCallback).onSuccess();
        verify(mockCompleteCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC103: addMember — batch fail → onError
    // -----------------------------------------------------------------------

    @Test
    public void addMember_batchFails_callsOnError() {
        Exception exception = new Exception("Add member error");
        Task<Void> failTask = buildVoidFailureTask(exception);
        when(mockBatch.commit()).thenReturn(failTask);

        repository.addMember("conv1", "uid2", mockCompleteCallback);

        verify(mockCompleteCallback).onError("Add member error");
        verify(mockCompleteCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC104: removeMember — convId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void removeMember_nullConvId_callsOnError() {
        repository.removeMember(null, "uid2", "uid1", mockCompleteCallback);

        verify(mockCompleteCallback).onError("ID hội thoại không hợp lệ.");
        verify(mockCompleteCallback, never()).onSuccess();
        verify(mockMemberRef, never()).get();
    }

    // -----------------------------------------------------------------------
    // TC105: removeMember — userId null/rỗng → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void removeMember_nullUserId_callsOnError() {
        repository.removeMember("conv1", null, "uid1", mockCompleteCallback);

        verify(mockCompleteCallback).onError("ID thành viên không hợp lệ.");
        verify(mockCompleteCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC106: removeMember — requesterId null/rỗng → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void removeMember_nullRequesterId_callsOnError() {
        repository.removeMember("conv1", "uid2", null, mockCompleteCallback);

        verify(mockCompleteCallback).onError("ID người yêu cầu không hợp lệ.");
        verify(mockCompleteCallback, never()).onSuccess();
        verify(mockMemberRef, never()).get();
    }

    // -----------------------------------------------------------------------
    // TC107: removeMember — requester không phải thành viên (snapshot không tồn tại)
    // -----------------------------------------------------------------------

    @Test
    public void removeMember_requesterNotMember_callsOnError() {
        when(mockMemberSnapshot.exists()).thenReturn(false);
        Task<DocumentSnapshot> getTask = buildDocSnapshotSuccessTask(mockMemberSnapshot);
        when(mockMemberRef.get()).thenReturn(getTask);

        repository.removeMember("conv1", "uid2", "uid1", mockCompleteCallback);

        verify(mockCompleteCallback).onError("Người yêu cầu không phải thành viên nhóm.");
        verify(mockCompleteCallback, never()).onSuccess();
        verify(mockFirestore, never()).batch();
    }

    // -----------------------------------------------------------------------
    // TC108: removeMember — requester là member (không phải admin) → onError
    // -----------------------------------------------------------------------

    @Test
    public void removeMember_requesterNotAdmin_callsOnError() {
        when(mockMemberSnapshot.exists()).thenReturn(true);
        when(mockMemberSnapshot.getString("role")).thenReturn("member");
        Task<DocumentSnapshot> getTask = buildDocSnapshotSuccessTask(mockMemberSnapshot);
        when(mockMemberRef.get()).thenReturn(getTask);

        repository.removeMember("conv1", "uid2", "uid1", mockCompleteCallback);

        verify(mockCompleteCallback).onError("Chỉ admin mới có thể xóa thành viên.");
        verify(mockCompleteCallback, never()).onSuccess();
        verify(mockFirestore, never()).batch();
    }

    // -----------------------------------------------------------------------
    // TC109: removeMember — requester là admin → batch.update + batch.delete + onSuccess()
    // -----------------------------------------------------------------------

    @Test
    public void removeMember_requesterIsAdmin_batchUpdateDeleteAndOnSuccess() {
        when(mockMemberSnapshot.exists()).thenReturn(true);
        when(mockMemberSnapshot.getString("role")).thenReturn("admin");
        Task<DocumentSnapshot> getTask = buildDocSnapshotSuccessTask(mockMemberSnapshot);
        when(mockMemberRef.get()).thenReturn(getTask);

        Task<Void> commitTask = buildVoidSuccessTask();
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.removeMember("conv1", "uid2", "uid1", mockCompleteCallback);

        verify(mockBatch).update(eq(mockDocument), anyMap());
        verify(mockBatch).delete(eq(mockMemberRef));
        verify(mockBatch).commit();
        verify(mockCompleteCallback).onSuccess();
        verify(mockCompleteCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC110: removeMember — get() Firestore fail → onError
    // -----------------------------------------------------------------------

    @Test
    public void removeMember_getRequesterFails_callsOnError() {
        Exception exception = new Exception("Network error");
        Task<DocumentSnapshot> getTask = buildDocSnapshotFailureTask(exception);
        when(mockMemberRef.get()).thenReturn(getTask);

        repository.removeMember("conv1", "uid2", "uid1", mockCompleteCallback);

        verify(mockCompleteCallback).onError("Network error");
        verify(mockCompleteCallback, never()).onSuccess();
        verify(mockFirestore, never()).batch();
    }

    // -----------------------------------------------------------------------
    // TC111: removeMember — requester admin nhưng batch fail → onError
    // -----------------------------------------------------------------------

    @Test
    public void removeMember_adminBatchFails_callsOnError() {
        when(mockMemberSnapshot.exists()).thenReturn(true);
        when(mockMemberSnapshot.getString("role")).thenReturn("admin");
        Task<DocumentSnapshot> getTask = buildDocSnapshotSuccessTask(mockMemberSnapshot);
        when(mockMemberRef.get()).thenReturn(getTask);

        Exception exception = new Exception("Batch commit error");
        Task<Void> failTask = buildVoidFailureTask(exception);
        when(mockBatch.commit()).thenReturn(failTask);

        repository.removeMember("conv1", "uid2", "uid1", mockCompleteCallback);

        verify(mockCompleteCallback).onError("Batch commit error");
        verify(mockCompleteCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC112: setMemberRole — convId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void setMemberRole_nullConvId_callsOnError() {
        repository.setMemberRole(null, "uid2", "uid1", "admin", mockCompleteCallback);

        verify(mockCompleteCallback).onError("ID hội thoại không hợp lệ.");
        verify(mockCompleteCallback, never()).onSuccess();
        verify(mockMemberRef, never()).get();
    }

    // -----------------------------------------------------------------------
    // TC113: setMemberRole — memberId null/rỗng → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void setMemberRole_nullMemberId_callsOnError() {
        repository.setMemberRole("conv1", null, "uid1", "admin", mockCompleteCallback);

        verify(mockCompleteCallback).onError("ID thành viên không hợp lệ.");
        verify(mockCompleteCallback, never()).onSuccess();
    }

    @Test
    public void setMemberRole_emptyMemberId_callsOnError() {
        repository.setMemberRole("conv1", "  ", "uid1", "admin", mockCompleteCallback);

        verify(mockCompleteCallback).onError("ID thành viên không hợp lệ.");
        verify(mockCompleteCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC114: setMemberRole — requesterId null/rỗng → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void setMemberRole_nullRequesterId_callsOnError() {
        repository.setMemberRole("conv1", "uid2", null, "admin", mockCompleteCallback);

        verify(mockCompleteCallback).onError("ID người yêu cầu không hợp lệ.");
        verify(mockCompleteCallback, never()).onSuccess();
        verify(mockMemberRef, never()).get();
    }

    // -----------------------------------------------------------------------
    // TC115: setMemberRole — role không hợp lệ → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void setMemberRole_invalidRole_callsOnError() {
        repository.setMemberRole("conv1", "uid2", "uid1", "superadmin", mockCompleteCallback);

        verify(mockCompleteCallback).onError(
                "Vai trò không hợp lệ. Chỉ chấp nhận \"admin\" hoặc \"member\".");
        verify(mockCompleteCallback, never()).onSuccess();
        verify(mockMemberRef, never()).get();
    }

    // -----------------------------------------------------------------------
    // TC116: setMemberRole — requester không phải thành viên (snapshot không tồn tại)
    // -----------------------------------------------------------------------

    @Test
    public void setMemberRole_requesterNotMember_callsOnError() {
        when(mockMemberSnapshot.exists()).thenReturn(false);
        Task<DocumentSnapshot> getTask = buildDocSnapshotSuccessTask(mockMemberSnapshot);
        when(mockMemberRef.get()).thenReturn(getTask);

        repository.setMemberRole("conv1", "uid2", "uid1", "admin", mockCompleteCallback);

        verify(mockCompleteCallback).onError("Người yêu cầu không phải thành viên nhóm.");
        verify(mockCompleteCallback, never()).onSuccess();
        verify(mockFirestore, never()).batch();
    }

    // -----------------------------------------------------------------------
    // TC117: setMemberRole — requester là member (không phải admin) → onError
    // -----------------------------------------------------------------------

    @Test
    public void setMemberRole_requesterNotAdmin_callsOnError() {
        when(mockMemberSnapshot.exists()).thenReturn(true);
        when(mockMemberSnapshot.getString("role")).thenReturn("member");
        Task<DocumentSnapshot> getTask = buildDocSnapshotSuccessTask(mockMemberSnapshot);
        when(mockMemberRef.get()).thenReturn(getTask);

        repository.setMemberRole("conv1", "uid2", "uid1", "admin", mockCompleteCallback);

        verify(mockCompleteCallback).onError("Chỉ admin mới có thể thay đổi vai trò thành viên.");
        verify(mockCompleteCallback, never()).onSuccess();
        verify(mockFirestore, never()).batch();
    }

    // -----------------------------------------------------------------------
    // TC118: setMemberRole — thăng lên admin → batch.update x2 + arrayUnion + onSuccess()
    // -----------------------------------------------------------------------

    @Test
    public void setMemberRole_promoteToAdmin_batchUpdatesAndOnSuccess() {
        when(mockMemberSnapshot.exists()).thenReturn(true);
        when(mockMemberSnapshot.getString("role")).thenReturn("admin");
        Task<DocumentSnapshot> getTask = buildDocSnapshotSuccessTask(mockMemberSnapshot);
        when(mockMemberRef.get()).thenReturn(getTask);

        Task<Void> commitTask = buildVoidSuccessTask();
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.setMemberRole("conv1", "uid2", "uid1", "admin", mockCompleteCallback);

        // Không cần đọc conv doc khi promote
        verify(mockDocument, never()).get();
        verify(mockBatch, times(2)).update(any(DocumentReference.class), anyMap());
        verify(mockBatch).commit();
        verify(mockCompleteCallback).onSuccess();
        verify(mockCompleteCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC119: setMemberRole — hạ xuống member, không phải admin cuối → onSuccess()
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    public void setMemberRole_demoteToMember_notLastAdmin_batchUpdatesAndOnSuccess() {
        when(mockMemberSnapshot.exists()).thenReturn(true);
        when(mockMemberSnapshot.getString("role")).thenReturn("admin");
        Task<DocumentSnapshot> memberGetTask = buildDocSnapshotSuccessTask(mockMemberSnapshot);
        when(mockMemberRef.get()).thenReturn(memberGetTask);

        // Conv doc có 2 admin → không phải admin cuối
        when(mockConvSnapshot.get("adminIds")).thenReturn(Arrays.asList("uid1", "uid2"));
        Task<DocumentSnapshot> convGetTask = buildDocSnapshotSuccessTask(mockConvSnapshot);
        when(mockDocument.get()).thenReturn(convGetTask);

        Task<Void> commitTask = buildVoidSuccessTask();
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.setMemberRole("conv1", "uid2", "uid1", "member", mockCompleteCallback);

        verify(mockDocument).get();
        verify(mockBatch, times(2)).update(any(DocumentReference.class), anyMap());
        verify(mockBatch).commit();
        verify(mockCompleteCallback).onSuccess();
        verify(mockCompleteCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC120: setMemberRole — hạ cấp admin cuối → onError (không thể hạ)
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    public void setMemberRole_demoteLastAdmin_callsOnError() {
        when(mockMemberSnapshot.exists()).thenReturn(true);
        when(mockMemberSnapshot.getString("role")).thenReturn("admin");
        Task<DocumentSnapshot> memberGetTask = buildDocSnapshotSuccessTask(mockMemberSnapshot);
        when(mockMemberRef.get()).thenReturn(memberGetTask);

        // Conv doc chỉ có 1 admin = memberId đang bị hạ
        when(mockConvSnapshot.get("adminIds")).thenReturn(java.util.Collections.singletonList("uid1"));
        Task<DocumentSnapshot> convGetTask = buildDocSnapshotSuccessTask(mockConvSnapshot);
        when(mockDocument.get()).thenReturn(convGetTask);

        repository.setMemberRole("conv1", "uid1", "uid1", "member", mockCompleteCallback);

        verify(mockCompleteCallback).onError(
                "Không thể hạ cấp admin cuối cùng. Hãy chỉ định admin khác trước.");
        verify(mockCompleteCallback, never()).onSuccess();
        verify(mockFirestore, never()).batch();
    }

    // -----------------------------------------------------------------------
    // TC121: setMemberRole — requester.get() Firestore fail → onError
    // -----------------------------------------------------------------------

    @Test
    public void setMemberRole_getRequesterFails_callsOnError() {
        Exception exception = new Exception("Firestore network error");
        Task<DocumentSnapshot> failTask = buildDocSnapshotFailureTask(exception);
        when(mockMemberRef.get()).thenReturn(failTask);

        repository.setMemberRole("conv1", "uid2", "uid1", "admin", mockCompleteCallback);

        verify(mockCompleteCallback).onError("Firestore network error");
        verify(mockCompleteCallback, never()).onSuccess();
        verify(mockFirestore, never()).batch();
    }

    // -----------------------------------------------------------------------
    // TC122: setMemberRole — convRef.get() fail khi kiểm tra admin cuối → onError
    // -----------------------------------------------------------------------

    @Test
    public void setMemberRole_getConvDocFails_callsOnError() {
        when(mockMemberSnapshot.exists()).thenReturn(true);
        when(mockMemberSnapshot.getString("role")).thenReturn("admin");
        Task<DocumentSnapshot> memberGetTask = buildDocSnapshotSuccessTask(mockMemberSnapshot);
        when(mockMemberRef.get()).thenReturn(memberGetTask);

        Exception exception = new Exception("Conv fetch error");
        Task<DocumentSnapshot> convFailTask = buildDocSnapshotFailureTask(exception);
        when(mockDocument.get()).thenReturn(convFailTask);

        repository.setMemberRole("conv1", "uid2", "uid1", "member", mockCompleteCallback);

        verify(mockCompleteCallback).onError("Conv fetch error");
        verify(mockCompleteCallback, never()).onSuccess();
        verify(mockFirestore, never()).batch();
    }

    // -----------------------------------------------------------------------
    // TC123: setMemberRole — batch commit fail → onError
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    public void setMemberRole_batchCommitFails_callsOnError() {
        when(mockMemberSnapshot.exists()).thenReturn(true);
        when(mockMemberSnapshot.getString("role")).thenReturn("admin");
        Task<DocumentSnapshot> memberGetTask = buildDocSnapshotSuccessTask(mockMemberSnapshot);
        when(mockMemberRef.get()).thenReturn(memberGetTask);

        Exception exception = new Exception("Batch commit error");
        Task<Void> failTask = buildVoidFailureTask(exception);
        when(mockBatch.commit()).thenReturn(failTask);

        repository.setMemberRole("conv1", "uid2", "uid1", "admin", mockCompleteCallback);

        verify(mockCompleteCallback).onError("Batch commit error");
        verify(mockCompleteCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // Helper: tạo mock Task<DocumentSnapshot> thành công
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Task<DocumentSnapshot> buildDocSnapshotSuccessTask(DocumentSnapshot snapshot) {
        Task<DocumentSnapshot> mockTask = mock(Task.class);
        doAnswer(inv -> {
            ((OnSuccessListener<DocumentSnapshot>) inv.getArgument(0)).onSuccess(snapshot);
            return mockTask;
        }).when(mockTask).addOnSuccessListener(any(OnSuccessListener.class));
        when(mockTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockTask);
        return mockTask;
    }

    // -----------------------------------------------------------------------
    // Helper: tạo mock Task<DocumentSnapshot> thất bại
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Task<DocumentSnapshot> buildDocSnapshotFailureTask(Exception exception) {
        Task<DocumentSnapshot> mockTask = mock(Task.class);
        when(mockTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(mockTask);
        doAnswer(inv -> {
            ((OnFailureListener) inv.getArgument(0)).onFailure(exception);
            return mockTask;
        }).when(mockTask).addOnFailureListener(any(OnFailureListener.class));
        return mockTask;
    }
}
