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

    @Mock
    private ConversationRepository.OnConversationCallback mockCallback;

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
}
