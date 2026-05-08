package com.example.beechats.data.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.beechats.data.models.Message;
import com.example.beechats.data.models.ReplyInfo;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Unit tests cho MessageRepository.
 * Mock Firestore + WriteBatch chain để test không cần device/emulator.
 */
public class MessageRepositoryTest {

    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private CollectionReference mockCollection;

    /** conversations/{id} document — dùng cả để lấy subcollection messages và làm convRef */
    @Mock
    private DocumentReference mockConvDocRef;

    @Mock
    private CollectionReference mockMsgSubCollection;

    /** message document mới (auto-ID) */
    @Mock
    private DocumentReference mockMsgRef;

    @Mock
    private WriteBatch mockBatch;

    @Mock
    private MessageRepository.OnSendMessageCallback mockCallback;

    @Mock
    private Query mockQuery;

    @Mock
    private ListenerRegistration mockListenerReg;

    @Mock
    private QuerySnapshot mockQuerySnapshot;

    @Mock
    private MessageRepository.OnMessagesCallback mockMsgCallback;

    /** DocumentReference cho message cụ thể (markDelivered dùng .document(messageId)) */
    @Mock
    private DocumentReference mockSpecificMsgRef;

    /** Query trả về từ .whereIn() (markAsRead) */
    @Mock
    private Query mockWhereInQuery;

    /** Callback cho markDelivered / markAsRead */
    @Mock
    private MessageRepository.OnMessageStatusCallback mockStatusCallback;

    /** DocumentSnapshot trả về khi fetch message document (recallMessage) */
    @Mock
    private DocumentSnapshot mockMsgSnapshot;

    /** DocumentSnapshot trả về khi fetch member document (pinMessage/unpinMessage) */
    @Mock
    private DocumentSnapshot mockMemberSnapshot;

    @Captor
    private ArgumentCaptor<Map<String, Object>> mapCaptor;

    private MessageRepository repository;

    private static final String CONV_ID = "aaaa_bbbb";
    private static final String SENDER_ID = "aaaa";
    private static final String SENDER_NAME = "User A Test";
    private static final String MESSAGE_TEXT = "Xin chào";
    private static final String TEST_MESSAGE_ID = "testMessageId001";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Stub Firestore chain:
        // db.collection("conversations").document(convId) → mockConvDocRef
        // mockConvDocRef.collection("messages").document() → mockMsgRef
        when(mockFirestore.collection(anyString())).thenReturn(mockCollection);
        when(mockCollection.document(anyString())).thenReturn(mockConvDocRef);
        when(mockConvDocRef.collection(anyString())).thenReturn(mockMsgSubCollection);
        when(mockMsgSubCollection.document()).thenReturn(mockMsgRef); // no-arg auto-ID

        when(mockMsgRef.getId()).thenReturn(TEST_MESSAGE_ID);
        when(mockFirestore.batch()).thenReturn(mockBatch);
        when(mockBatch.set(any(DocumentReference.class), anyMap())).thenReturn(mockBatch);
        when(mockBatch.update(any(DocumentReference.class), anyMap())).thenReturn(mockBatch);

        // Stub chain cho listenToMessages: mockMsgSubCollection.orderBy().limit() → mockQuery
        when(mockMsgSubCollection.orderBy(anyString(), any(Query.Direction.class))).thenReturn(mockQuery);
        when(mockQuery.limit(anyLong())).thenReturn(mockQuery);

        // Stub cho markDelivered: mockMsgSubCollection.document(messageId) → mockSpecificMsgRef
        when(mockMsgSubCollection.document(anyString())).thenReturn(mockSpecificMsgRef);

        // Stub cho markAsRead: mockMsgSubCollection.whereIn(...) → mockWhereInQuery
        when(mockMsgSubCollection.whereIn(anyString(), any())).thenReturn(mockWhereInQuery);

        repository = new MessageRepository(mockFirestore);
    }

    // -----------------------------------------------------------------------
    // TC31: Happy path — tham số hợp lệ → batch.commit() gọi → onSuccess(messageId)
    // -----------------------------------------------------------------------

    @Test
    public void sendMessage_validParams_callsCommitAndOnSuccess() {
        Task<Void> commitTask = buildVoidSuccessTask();
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.sendMessage(CONV_ID, SENDER_ID, SENDER_NAME, MESSAGE_TEXT, mockCallback);

        verify(mockBatch).commit();
        verify(mockCallback).onSuccess(TEST_MESSAGE_ID);
        verify(mockCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC32: lastMessage.text trong batch.update() khớp với text đã gửi
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    public void sendMessage_validParams_lastMessageTextMatchesSentText() {
        Task<Void> commitTask = buildVoidSuccessTask();
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.sendMessage(CONV_ID, SENDER_ID, SENDER_NAME, MESSAGE_TEXT, mockCallback);

        // Bắt map truyền vào batch.update(convRef, convUpdate)
        verify(mockBatch).update(any(DocumentReference.class), mapCaptor.capture());
        Map<String, Object> convUpdate = mapCaptor.getValue();
        Map<String, Object> lastMessage = (Map<String, Object>) convUpdate.get("lastMessage");

        assertNotNull(lastMessage);
        assertEquals(MESSAGE_TEXT, lastMessage.get("text"));
        assertEquals(SENDER_ID, lastMessage.get("senderId"));
    }

    // -----------------------------------------------------------------------
    // TC33: convUpdate trong batch.update() chứa trường updatedAt
    // -----------------------------------------------------------------------

    @Test
    public void sendMessage_validParams_convUpdateContainsUpdatedAt() {
        Task<Void> commitTask = buildVoidSuccessTask();
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.sendMessage(CONV_ID, SENDER_ID, SENDER_NAME, MESSAGE_TEXT, mockCallback);

        verify(mockBatch).update(any(DocumentReference.class), mapCaptor.capture());
        Map<String, Object> convUpdate = mapCaptor.getValue();

        assertTrue("convUpdate phải chứa trường updatedAt", convUpdate.containsKey("updatedAt"));
    }

    // -----------------------------------------------------------------------
    // TC34: text rỗng/null → onError ngay, Firestore không gọi
    // -----------------------------------------------------------------------

    @Test
    public void sendMessage_emptyText_callsOnErrorWithoutFirestore() {
        repository.sendMessage(CONV_ID, SENDER_ID, SENDER_NAME, "", mockCallback);

        verify(mockCallback).onError("Nội dung tin nhắn không được để trống.");
        verify(mockCallback, never()).onSuccess(anyString());
        verify(mockFirestore, never()).batch();
    }

    @Test
    public void sendMessage_nullText_callsOnErrorWithoutFirestore() {
        repository.sendMessage(CONV_ID, SENDER_ID, SENDER_NAME, null, mockCallback);

        verify(mockCallback).onError("Nội dung tin nhắn không được để trống.");
        verify(mockCallback, never()).onSuccess(anyString());
        verify(mockFirestore, never()).batch();
    }

    // -----------------------------------------------------------------------
    // TC35: conversationId null/rỗng → onError ngay, Firestore không gọi
    // -----------------------------------------------------------------------

    @Test
    public void sendMessage_nullConversationId_callsOnErrorWithoutFirestore() {
        repository.sendMessage(null, SENDER_ID, SENDER_NAME, MESSAGE_TEXT, mockCallback);

        verify(mockCallback).onError("ID hội thoại không hợp lệ.");
        verify(mockCallback, never()).onSuccess(anyString());
        verify(mockFirestore, never()).batch();
    }

    @Test
    public void sendMessage_emptyConversationId_callsOnErrorWithoutFirestore() {
        repository.sendMessage("", SENDER_ID, SENDER_NAME, MESSAGE_TEXT, mockCallback);

        verify(mockCallback).onError("ID hội thoại không hợp lệ.");
        verify(mockCallback, never()).onSuccess(anyString());
        verify(mockFirestore, never()).batch();
    }

    // -----------------------------------------------------------------------
    // TC36: batch.commit() fail → onError(message), onSuccess() không gọi
    // -----------------------------------------------------------------------

    @Test
    public void sendMessage_commitFails_callsOnErrorWithMessage() {
        Exception exception = new Exception("Firestore network error");
        Task<Void> commitTask = buildVoidFailureTask(exception);
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.sendMessage(CONV_ID, SENDER_ID, SENDER_NAME, MESSAGE_TEXT, mockCallback);

        verify(mockCallback).onError("Firestore network error");
        verify(mockCallback, never()).onSuccess(anyString());
    }

    // -----------------------------------------------------------------------
    // TC37: Snapshot có 2 documents → onSuccess(list) với size=2, messageId được set
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    public void listenToMessages_snapshotWithTwoDocs_callsOnSuccessWithList() {
        DocumentSnapshot mockDoc1 = mock(DocumentSnapshot.class);
        DocumentSnapshot mockDoc2 = mock(DocumentSnapshot.class);
        Message msg1 = new Message();
        msg1.setText("Xin chào");
        Message msg2 = new Message();
        msg2.setText("Hello");
        when(mockDoc1.getId()).thenReturn("msgId1");
        when(mockDoc1.toObject(Message.class)).thenReturn(msg1);
        when(mockDoc2.getId()).thenReturn("msgId2");
        when(mockDoc2.toObject(Message.class)).thenReturn(msg2);
        when(mockQuerySnapshot.getDocuments()).thenReturn(Arrays.asList(mockDoc1, mockDoc2));

        // Trigger EventListener ngay khi addSnapshotListener() được gọi
        doAnswer(inv -> {
            EventListener<QuerySnapshot> listener = inv.getArgument(0);
            listener.onEvent(mockQuerySnapshot, null);
            return mockListenerReg;
        }).when(mockQuery).addSnapshotListener(any(EventListener.class));

        repository.listenToMessages(CONV_ID, mockMsgCallback);

        ArgumentCaptor<List<Message>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockMsgCallback).onSuccess(listCaptor.capture());
        verify(mockMsgCallback, never()).onError(anyString());
        assertEquals(2, listCaptor.getValue().size());
        assertEquals("msgId1", listCaptor.getValue().get(0).getMessageId());
        assertEquals("msgId2", listCaptor.getValue().get(1).getMessageId());
    }

    // -----------------------------------------------------------------------
    // TC38: listenToMessages() trả về ListenerRegistration không null
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    public void listenToMessages_validConvId_returnsNonNullRegistration() {
        when(mockQuery.addSnapshotListener(any(EventListener.class))).thenReturn(mockListenerReg);

        ListenerRegistration reg = repository.listenToMessages(CONV_ID, mockMsgCallback);

        assertNotNull(reg);
    }

    // -----------------------------------------------------------------------
    // TC39: conversationId null/rỗng → onError ngay, Firestore không gọi
    // -----------------------------------------------------------------------

    @Test
    public void listenToMessages_nullConversationId_callsOnErrorWithoutFirestore() {
        ListenerRegistration reg = repository.listenToMessages(null, mockMsgCallback);

        verify(mockMsgCallback).onError("ID hội thoại không hợp lệ.");
        verify(mockMsgCallback, never()).onSuccess(any());
        assertNull(reg);
    }

    @Test
    public void listenToMessages_emptyConversationId_callsOnErrorWithoutFirestore() {
        ListenerRegistration reg = repository.listenToMessages("", mockMsgCallback);

        verify(mockMsgCallback).onError("ID hội thoại không hợp lệ.");
        verify(mockMsgCallback, never()).onSuccess(any());
        assertNull(reg);
    }

    // -----------------------------------------------------------------------
    // TC40: EventListener nhận FirebaseFirestoreException → onError(message)
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    public void listenToMessages_firestoreError_callsOnError() {
        FirebaseFirestoreException mockException = mock(FirebaseFirestoreException.class);
        when(mockException.getMessage()).thenReturn("Permission denied");

        doAnswer(inv -> {
            EventListener<QuerySnapshot> listener = inv.getArgument(0);
            listener.onEvent(null, mockException);
            return mockListenerReg;
        }).when(mockQuery).addSnapshotListener(any(EventListener.class));

        repository.listenToMessages(CONV_ID, mockMsgCallback);

        verify(mockMsgCallback).onError("Permission denied");
        verify(mockMsgCallback, never()).onSuccess(any());
    }

    // -----------------------------------------------------------------------
    // TC41: Snapshot rỗng (0 documents) → onSuccess(emptyList)
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    public void listenToMessages_emptySnapshot_callsOnSuccessWithEmptyList() {
        when(mockQuerySnapshot.getDocuments()).thenReturn(Collections.emptyList());

        doAnswer(inv -> {
            EventListener<QuerySnapshot> listener = inv.getArgument(0);
            listener.onEvent(mockQuerySnapshot, null);
            return mockListenerReg;
        }).when(mockQuery).addSnapshotListener(any(EventListener.class));

        repository.listenToMessages(CONV_ID, mockMsgCallback);

        ArgumentCaptor<List<Message>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockMsgCallback).onSuccess(listCaptor.capture());
        assertTrue(listCaptor.getValue().isEmpty());
    }

    // -----------------------------------------------------------------------
    // TC42: markDelivered — tham số hợp lệ → update() gọi → onSuccess()
    // -----------------------------------------------------------------------

    @Test
    public void markDelivered_validParams_callsUpdateAndOnSuccess() {
        Task<Void> updateTask = buildVoidSuccessTask();
        when(mockSpecificMsgRef.update(anyMap())).thenReturn(updateTask);

        repository.markDelivered(CONV_ID, TEST_MESSAGE_ID, mockStatusCallback);

        verify(mockSpecificMsgRef).update(anyMap());
        verify(mockStatusCallback).onSuccess();
        verify(mockStatusCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC43: markDelivered — conversationId null/rỗng → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void markDelivered_nullConversationId_callsOnErrorWithoutFirestore() {
        repository.markDelivered(null, TEST_MESSAGE_ID, mockStatusCallback);

        verify(mockStatusCallback).onError("ID hội thoại không hợp lệ.");
        verify(mockStatusCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    @Test
    public void markDelivered_emptyConversationId_callsOnErrorWithoutFirestore() {
        repository.markDelivered("", TEST_MESSAGE_ID, mockStatusCallback);

        verify(mockStatusCallback).onError("ID hội thoại không hợp lệ.");
        verify(mockStatusCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    // -----------------------------------------------------------------------
    // TC44: markDelivered — messageId null/rỗng → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void markDelivered_nullMessageId_callsOnErrorWithoutFirestore() {
        repository.markDelivered(CONV_ID, null, mockStatusCallback);

        verify(mockStatusCallback).onError("ID tin nhắn không hợp lệ.");
        verify(mockStatusCallback, never()).onSuccess();
    }

    @Test
    public void markDelivered_emptyMessageId_callsOnErrorWithoutFirestore() {
        repository.markDelivered(CONV_ID, "", mockStatusCallback);

        verify(mockStatusCallback).onError("ID tin nhắn không hợp lệ.");
        verify(mockStatusCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC45: markDelivered — Firestore update() fails → onError(message)
    // -----------------------------------------------------------------------

    @Test
    public void markDelivered_firestoreFails_callsOnError() {
        Exception exception = new Exception("Network error");
        Task<Void> updateTask = buildVoidFailureTask(exception);
        when(mockSpecificMsgRef.update(anyMap())).thenReturn(updateTask);

        repository.markDelivered(CONV_ID, TEST_MESSAGE_ID, mockStatusCallback);

        verify(mockStatusCallback).onError("Network error");
        verify(mockStatusCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC46: markAsRead — 2 tin chưa đọc → batch.commit() gọi 2 lần update → onSuccess()
    // -----------------------------------------------------------------------

    @Test
    public void markAsRead_withUnreadMessages_callsBatchCommitAndOnSuccess() {
        DocumentSnapshot mockUnreadDoc1 = mock(DocumentSnapshot.class);
        DocumentSnapshot mockUnreadDoc2 = mock(DocumentSnapshot.class);
        DocumentReference mockDocRef1 = mock(DocumentReference.class);
        DocumentReference mockDocRef2 = mock(DocumentReference.class);
        when(mockUnreadDoc1.getReference()).thenReturn(mockDocRef1);
        when(mockUnreadDoc2.getReference()).thenReturn(mockDocRef2);

        QuerySnapshot mockStatusSnapshot = mock(QuerySnapshot.class);
        when(mockStatusSnapshot.isEmpty()).thenReturn(false);
        when(mockStatusSnapshot.getDocuments()).thenReturn(Arrays.asList(mockUnreadDoc1, mockUnreadDoc2));

        // Tạo task trước, sau đó mới stub — tránh UnfinishedStubbingException
        Task<QuerySnapshot> queryTask46 = buildQuerySuccessTask(mockStatusSnapshot);
        when(mockWhereInQuery.get()).thenReturn(queryTask46);

        Task<Void> commitTask = buildVoidSuccessTask();
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.markAsRead(CONV_ID, "userB", mockStatusCallback);

        // Mỗi doc được update 1 lần, tổng 2 lần
        verify(mockBatch, times(2)).update(any(DocumentReference.class), anyMap());
        verify(mockBatch).commit();
        verify(mockStatusCallback).onSuccess();
        verify(mockStatusCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC47: markAsRead — không có tin chưa đọc → onSuccess() ngay, batch.commit() không gọi
    // -----------------------------------------------------------------------

    @Test
    public void markAsRead_noUnreadMessages_callsOnSuccessWithoutBatch() {
        QuerySnapshot mockStatusSnapshot = mock(QuerySnapshot.class);
        when(mockStatusSnapshot.isEmpty()).thenReturn(true);

        // Tạo task trước, sau đó mới stub — tránh UnfinishedStubbingException
        Task<QuerySnapshot> queryTask47 = buildQuerySuccessTask(mockStatusSnapshot);
        when(mockWhereInQuery.get()).thenReturn(queryTask47);

        repository.markAsRead(CONV_ID, "userB", mockStatusCallback);

        verify(mockBatch, never()).commit();
        verify(mockStatusCallback).onSuccess();
        verify(mockStatusCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC48: markAsRead — conversationId null/rỗng → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void markAsRead_nullConversationId_callsOnErrorWithoutFirestore() {
        repository.markAsRead(null, "userB", mockStatusCallback);

        verify(mockStatusCallback).onError("ID hội thoại không hợp lệ.");
        verify(mockStatusCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    @Test
    public void markAsRead_emptyConversationId_callsOnErrorWithoutFirestore() {
        repository.markAsRead("", "userB", mockStatusCallback);

        verify(mockStatusCallback).onError("ID hội thoại không hợp lệ.");
        verify(mockStatusCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    // -----------------------------------------------------------------------
    // TC49: markAsRead — userId null/rỗng → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void markAsRead_nullUserId_callsOnErrorWithoutFirestore() {
        repository.markAsRead(CONV_ID, null, mockStatusCallback);

        verify(mockStatusCallback).onError("ID người dùng không hợp lệ.");
        verify(mockStatusCallback, never()).onSuccess();
    }

    @Test
    public void markAsRead_emptyUserId_callsOnErrorWithoutFirestore() {
        repository.markAsRead(CONV_ID, "", mockStatusCallback);

        verify(mockStatusCallback).onError("ID người dùng không hợp lệ.");
        verify(mockStatusCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC50: markAsRead — Firestore query .get() fails → onError(message)
    // -----------------------------------------------------------------------

    @Test
    public void markAsRead_queryFails_callsOnError() {
        Exception exception = new Exception("Query failed");
        // Tạo task trước, sau đó mới stub — tránh UnfinishedStubbingException
        Task<QuerySnapshot> queryTask50 = buildQueryFailureTask(exception);
        when(mockWhereInQuery.get()).thenReturn(queryTask50);

        repository.markAsRead(CONV_ID, "userB", mockStatusCallback);

        verify(mockStatusCallback).onError("Query failed");
        verify(mockStatusCallback, never()).onSuccess();
        verify(mockBatch, never()).commit();
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
    // Helper: tạo mock Task<QuerySnapshot> thành công
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Task<QuerySnapshot> buildQuerySuccessTask(QuerySnapshot snapshot) {
        Task<QuerySnapshot> mockTask = mock(Task.class);
        doAnswer(inv -> {
            ((OnSuccessListener<QuerySnapshot>) inv.getArgument(0)).onSuccess(snapshot);
            return mockTask;
        }).when(mockTask).addOnSuccessListener(any(OnSuccessListener.class));
        when(mockTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockTask);
        return mockTask;
    }

    // -----------------------------------------------------------------------
    // Helper: tạo mock Task<QuerySnapshot> thất bại
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Task<QuerySnapshot> buildQueryFailureTask(Exception exception) {
        Task<QuerySnapshot> mockTask = mock(Task.class);
        when(mockTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(mockTask);
        doAnswer(inv -> {
            ((OnFailureListener) inv.getArgument(0)).onFailure(exception);
            return mockTask;
        }).when(mockTask).addOnFailureListener(any(OnFailureListener.class));
        return mockTask;
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

    // -----------------------------------------------------------------------
    // TC57: recallMessage — conversationId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void recallMessage_nullConversationId_callsOnError() {
        repository.recallMessage(null, TEST_MESSAGE_ID, SENDER_ID, mockStatusCallback);

        verify(mockStatusCallback).onError("ID hội thoại không hợp lệ.");
        verify(mockStatusCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    // -----------------------------------------------------------------------
    // TC58: recallMessage — conversationId rỗng → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void recallMessage_emptyConversationId_callsOnError() {
        repository.recallMessage("", TEST_MESSAGE_ID, SENDER_ID, mockStatusCallback);

        verify(mockStatusCallback).onError("ID hội thoại không hợp lệ.");
        verify(mockStatusCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    // -----------------------------------------------------------------------
    // TC59: recallMessage — messageId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void recallMessage_nullMessageId_callsOnError() {
        repository.recallMessage(CONV_ID, null, SENDER_ID, mockStatusCallback);

        verify(mockStatusCallback).onError("ID tin nhắn không hợp lệ.");
        verify(mockStatusCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC60: recallMessage — callerUid null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void recallMessage_nullCallerUid_callsOnError() {
        repository.recallMessage(CONV_ID, TEST_MESSAGE_ID, null, mockStatusCallback);

        verify(mockStatusCallback).onError("ID người dùng không hợp lệ.");
        verify(mockStatusCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC61: recallMessage — document không tồn tại → onError("Tin nhắn không tồn tại.")
    // -----------------------------------------------------------------------

    @Test
    public void recallMessage_documentNotExists_callsOnError() {
        when(mockMsgSnapshot.exists()).thenReturn(false);
        Task<DocumentSnapshot> getTask = buildDocSnapshotSuccessTask(mockMsgSnapshot);
        when(mockSpecificMsgRef.get()).thenReturn(getTask);

        repository.recallMessage(CONV_ID, TEST_MESSAGE_ID, SENDER_ID, mockStatusCallback);

        verify(mockStatusCallback).onError("Tin nhắn không tồn tại.");
        verify(mockStatusCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC62: recallMessage — callerUid ≠ senderId → onError("Chỉ người gửi mới được thu hồi")
    // -----------------------------------------------------------------------

    @Test
    public void recallMessage_wrongSender_callsOnError() {
        when(mockMsgSnapshot.exists()).thenReturn(true);
        when(mockMsgSnapshot.getString("senderId")).thenReturn("otherUserId");
        Task<DocumentSnapshot> getTask = buildDocSnapshotSuccessTask(mockMsgSnapshot);
        when(mockSpecificMsgRef.get()).thenReturn(getTask);

        repository.recallMessage(CONV_ID, TEST_MESSAGE_ID, SENDER_ID, mockStatusCallback);

        verify(mockStatusCallback).onError("Chỉ người gửi mới được thu hồi tin nhắn.");
        verify(mockStatusCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC63: recallMessage — createdAt > 5 phút → onError("Chỉ có thể thu hồi trong 5 phút đầu.")
    // -----------------------------------------------------------------------

    @Test
    public void recallMessage_exceeds5Minutes_callsOnError() {
        // Tạo timestamp 10 phút trước
        com.google.firebase.Timestamp oldTimestamp = mock(com.google.firebase.Timestamp.class);
        when(oldTimestamp.toDate()).thenReturn(new java.util.Date(System.currentTimeMillis() - 10 * 60 * 1000L));

        when(mockMsgSnapshot.exists()).thenReturn(true);
        when(mockMsgSnapshot.getString("senderId")).thenReturn(SENDER_ID);
        when(mockMsgSnapshot.getTimestamp("createdAt")).thenReturn(oldTimestamp);
        Task<DocumentSnapshot> getTask = buildDocSnapshotSuccessTask(mockMsgSnapshot);
        when(mockSpecificMsgRef.get()).thenReturn(getTask);

        repository.recallMessage(CONV_ID, TEST_MESSAGE_ID, SENDER_ID, mockStatusCallback);

        verify(mockStatusCallback).onError("Chỉ có thể thu hồi trong 5 phút đầu.");
        verify(mockStatusCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC64: recallMessage — hợp lệ (sender đúng, < 5 phút) → update() gọi → onSuccess()
    // -----------------------------------------------------------------------

    @Test
    public void recallMessage_validParams_callsUpdateAndOnSuccess() {
        // Tạo timestamp 1 phút trước (còn trong cửa sổ 5 phút)
        com.google.firebase.Timestamp recentTimestamp = mock(com.google.firebase.Timestamp.class);
        when(recentTimestamp.toDate()).thenReturn(new java.util.Date(System.currentTimeMillis() - 60 * 1000L));

        when(mockMsgSnapshot.exists()).thenReturn(true);
        when(mockMsgSnapshot.getString("senderId")).thenReturn(SENDER_ID);
        when(mockMsgSnapshot.getTimestamp("createdAt")).thenReturn(recentTimestamp);
        when(mockMsgSnapshot.getReference()).thenReturn(mockSpecificMsgRef);

        Task<DocumentSnapshot> getTask = buildDocSnapshotSuccessTask(mockMsgSnapshot);
        when(mockSpecificMsgRef.get()).thenReturn(getTask);

        Task<Void> updateTask = buildVoidSuccessTask();
        when(mockSpecificMsgRef.update(anyMap())).thenReturn(updateTask);

        repository.recallMessage(CONV_ID, TEST_MESSAGE_ID, SENDER_ID, mockStatusCallback);

        verify(mockSpecificMsgRef).update(anyMap());
        verify(mockStatusCallback).onSuccess();
        verify(mockStatusCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC65: recallMessage — hợp lệ nhưng Firestore update() fails → onError(message)
    // -----------------------------------------------------------------------

    @Test
    public void recallMessage_firestoreUpdateFails_callsOnError() {
        com.google.firebase.Timestamp recentTimestamp = mock(com.google.firebase.Timestamp.class);
        when(recentTimestamp.toDate()).thenReturn(new java.util.Date(System.currentTimeMillis() - 60 * 1000L));

        when(mockMsgSnapshot.exists()).thenReturn(true);
        when(mockMsgSnapshot.getString("senderId")).thenReturn(SENDER_ID);
        when(mockMsgSnapshot.getTimestamp("createdAt")).thenReturn(recentTimestamp);
        when(mockMsgSnapshot.getReference()).thenReturn(mockSpecificMsgRef);

        Task<DocumentSnapshot> getTask = buildDocSnapshotSuccessTask(mockMsgSnapshot);
        when(mockSpecificMsgRef.get()).thenReturn(getTask);

        Exception exception = new Exception("Firestore update failed");
        Task<Void> updateTask = buildVoidFailureTask(exception);
        when(mockSpecificMsgRef.update(anyMap())).thenReturn(updateTask);

        repository.recallMessage(CONV_ID, TEST_MESSAGE_ID, SENDER_ID, mockStatusCallback);

        verify(mockStatusCallback).onError("Firestore update failed");
        verify(mockStatusCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC66: sendReplyMessage — conversationId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void sendReplyMessage_nullConversationId_callsOnError() {
        ReplyInfo replyTo = new ReplyInfo();
        replyTo.setMessageId("origMsgId");
        replyTo.setText("Text gốc");

        repository.sendReplyMessage(null, SENDER_ID, SENDER_NAME, MESSAGE_TEXT, replyTo, mockCallback);

        verify(mockCallback).onError("ID hội thoại không hợp lệ.");
        verify(mockCallback, never()).onSuccess(anyString());
        verify(mockFirestore, never()).collection(anyString());
    }

    // -----------------------------------------------------------------------
    // TC67: sendReplyMessage — replyTo null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void sendReplyMessage_nullReplyTo_callsOnError() {
        repository.sendReplyMessage(CONV_ID, SENDER_ID, SENDER_NAME, MESSAGE_TEXT, null, mockCallback);

        verify(mockCallback).onError("Thông tin tin nhắn gốc không hợp lệ.");
        verify(mockCallback, never()).onSuccess(anyString());
    }

    // -----------------------------------------------------------------------
    // TC68: sendReplyMessage — replyTo.messageId null/rỗng → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void sendReplyMessage_nullReplyToMessageId_callsOnError() {
        ReplyInfo replyTo = new ReplyInfo();
        replyTo.setMessageId(null);

        repository.sendReplyMessage(CONV_ID, SENDER_ID, SENDER_NAME, MESSAGE_TEXT, replyTo, mockCallback);

        verify(mockCallback).onError("ID tin nhắn gốc không hợp lệ.");
        verify(mockCallback, never()).onSuccess(anyString());
    }

    @Test
    public void sendReplyMessage_emptyReplyToMessageId_callsOnError() {
        ReplyInfo replyTo = new ReplyInfo();
        replyTo.setMessageId("");

        repository.sendReplyMessage(CONV_ID, SENDER_ID, SENDER_NAME, MESSAGE_TEXT, replyTo, mockCallback);

        verify(mockCallback).onError("ID tin nhắn gốc không hợp lệ.");
        verify(mockCallback, never()).onSuccess(anyString());
    }

    // -----------------------------------------------------------------------
    // TC69: sendReplyMessage — tham số hợp lệ → batch.commit() gọi → onSuccess(messageId)
    // -----------------------------------------------------------------------

    @Test
    public void sendReplyMessage_validParams_callsCommitAndOnSuccess() {
        ReplyInfo replyTo = new ReplyInfo();
        replyTo.setMessageId("origMsgId");
        replyTo.setText("Text gốc");
        replyTo.setSenderId("senderB");
        replyTo.setSenderName("User B");

        Task<Void> commitTask = buildVoidSuccessTask();
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.sendReplyMessage(CONV_ID, SENDER_ID, SENDER_NAME, MESSAGE_TEXT, replyTo, mockCallback);

        verify(mockBatch).commit();
        verify(mockCallback).onSuccess(TEST_MESSAGE_ID);
        verify(mockCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC70: sendReplyMessage — replyTo.messageId trong msgData khớp message gốc
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    public void sendReplyMessage_validParams_replyToMessageIdMatchesOriginal() {
        ReplyInfo replyTo = new ReplyInfo();
        replyTo.setMessageId("origMsgId");
        replyTo.setText("Text gốc");

        Task<Void> commitTask = buildVoidSuccessTask();
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.sendReplyMessage(CONV_ID, SENDER_ID, SENDER_NAME, MESSAGE_TEXT, replyTo, mockCallback);

        // Bắt msgData từ batch.set(msgRef, msgData)
        verify(mockBatch).set(any(DocumentReference.class), mapCaptor.capture());
        Map<String, Object> msgData = mapCaptor.getValue();
        Map<String, Object> replyToMap = (Map<String, Object>) msgData.get("replyTo");

        assertNotNull(replyToMap);
        assertEquals("origMsgId", replyToMap.get("messageId"));
    }

    // -----------------------------------------------------------------------
    // TC71: sendReplyMessage — replyTo.text là preview nội dung gốc (non-null)
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    public void sendReplyMessage_nonNullReplyText_storesOriginalText() {
        ReplyInfo replyTo = new ReplyInfo();
        replyTo.setMessageId("origMsgId");
        replyTo.setText("Text gốc");

        Task<Void> commitTask = buildVoidSuccessTask();
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.sendReplyMessage(CONV_ID, SENDER_ID, SENDER_NAME, MESSAGE_TEXT, replyTo, mockCallback);

        verify(mockBatch).set(any(DocumentReference.class), mapCaptor.capture());
        Map<String, Object> msgData = mapCaptor.getValue();
        Map<String, Object> replyToMap = (Map<String, Object>) msgData.get("replyTo");

        assertNotNull(replyToMap);
        assertEquals("Text gốc", replyToMap.get("text"));
    }

    // -----------------------------------------------------------------------
    // TC72: sendReplyMessage — replyTo.text null (tin đã thu hồi) → "[Tin nhắn đã thu hồi]"
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    public void sendReplyMessage_nullReplyText_storesRecalledPlaceholder() {
        ReplyInfo replyTo = new ReplyInfo();
        replyTo.setMessageId("origMsgId");
        replyTo.setText(null); // Tin gốc đã bị thu hồi, text đã bị xóa

        Task<Void> commitTask = buildVoidSuccessTask();
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.sendReplyMessage(CONV_ID, SENDER_ID, SENDER_NAME, MESSAGE_TEXT, replyTo, mockCallback);

        verify(mockBatch).set(any(DocumentReference.class), mapCaptor.capture());
        Map<String, Object> msgData = mapCaptor.getValue();
        Map<String, Object> replyToMap = (Map<String, Object>) msgData.get("replyTo");

        assertNotNull(replyToMap);
        assertEquals("[Tin nhắn đã thu hồi]", replyToMap.get("text"));
    }

    // -----------------------------------------------------------------------
    // TC73: sendReplyMessage — hợp lệ nhưng batch.commit() fails → onError(message)
    // -----------------------------------------------------------------------

    @Test
    public void sendReplyMessage_commitFails_callsOnError() {
        ReplyInfo replyTo = new ReplyInfo();
        replyTo.setMessageId("origMsgId");
        replyTo.setText("Text gốc");

        Exception exception = new Exception("Commit failed");
        Task<Void> commitTask = buildVoidFailureTask(exception);
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.sendReplyMessage(CONV_ID, SENDER_ID, SENDER_NAME, MESSAGE_TEXT, replyTo, mockCallback);

        verify(mockCallback).onError("Commit failed");
        verify(mockCallback, never()).onSuccess(anyString());
    }

    // -----------------------------------------------------------------------
    // TC74: addReaction — conversationId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void addReaction_nullConversationId_callsOnError() {
        repository.addReaction(null, TEST_MESSAGE_ID, SENDER_ID, "❤️", mockStatusCallback);

        verify(mockStatusCallback).onError("ID hội thoại không hợp lệ.");
        verify(mockStatusCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    // -----------------------------------------------------------------------
    // TC75: addReaction — messageId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void addReaction_nullMessageId_callsOnError() {
        repository.addReaction(CONV_ID, null, SENDER_ID, "❤️", mockStatusCallback);

        verify(mockStatusCallback).onError("ID tin nhắn không hợp lệ.");
        verify(mockStatusCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    // -----------------------------------------------------------------------
    // TC76: addReaction — userId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void addReaction_nullUserId_callsOnError() {
        repository.addReaction(CONV_ID, TEST_MESSAGE_ID, null, "❤️", mockStatusCallback);

        verify(mockStatusCallback).onError("ID người dùng không hợp lệ.");
        verify(mockStatusCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    // -----------------------------------------------------------------------
    // TC77: addReaction — emoji null/rỗng → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void addReaction_nullEmoji_callsOnError() {
        repository.addReaction(CONV_ID, TEST_MESSAGE_ID, SENDER_ID, null, mockStatusCallback);

        verify(mockStatusCallback).onError("Emoji không được để trống.");
        verify(mockStatusCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    @Test
    public void addReaction_emptyEmoji_callsOnError() {
        repository.addReaction(CONV_ID, TEST_MESSAGE_ID, SENDER_ID, "", mockStatusCallback);

        verify(mockStatusCallback).onError("Emoji không được để trống.");
        verify(mockStatusCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    // -----------------------------------------------------------------------
    // TC78: addReaction — params hợp lệ → update("reactions.aaaa", "❤️") → onSuccess()
    // -----------------------------------------------------------------------

    @Test
    public void addReaction_validParams_callsUpdateAndOnSuccess() {
        Task<Void> updateTask = buildVoidSuccessTask();
        // Stub update(String field, Object value) — overload khác với update(Map)
        when(mockSpecificMsgRef.update(anyString(), any(Object.class))).thenReturn(updateTask);

        ArgumentCaptor<String> fieldCaptor = ArgumentCaptor.forClass(String.class);

        repository.addReaction(CONV_ID, TEST_MESSAGE_ID, SENDER_ID, "❤️", mockStatusCallback);

        verify(mockSpecificMsgRef).update(fieldCaptor.capture(), any(Object.class));
        assertEquals("reactions." + SENDER_ID, fieldCaptor.getValue());
        verify(mockStatusCallback).onSuccess();
        verify(mockStatusCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC79: addReaction — update thất bại → onError(message)
    // -----------------------------------------------------------------------

    @Test
    public void addReaction_updateFails_callsOnError() {
        Exception exception = new Exception("Network error");
        Task<Void> updateTask = buildVoidFailureTask(exception);
        when(mockSpecificMsgRef.update(anyString(), any(Object.class))).thenReturn(updateTask);

        repository.addReaction(CONV_ID, TEST_MESSAGE_ID, SENDER_ID, "❤️", mockStatusCallback);

        verify(mockStatusCallback).onError("Network error");
        verify(mockStatusCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC80: removeReaction — conversationId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void removeReaction_nullConversationId_callsOnError() {
        repository.removeReaction(null, TEST_MESSAGE_ID, SENDER_ID, mockStatusCallback);

        verify(mockStatusCallback).onError("ID hội thoại không hợp lệ.");
        verify(mockStatusCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    // -----------------------------------------------------------------------
    // TC81: removeReaction — params hợp lệ → update("reactions.aaaa", FieldValue.delete()) → onSuccess()
    // -----------------------------------------------------------------------

    @Test
    public void removeReaction_validParams_callsUpdateWithDeleteAndOnSuccess() {
        Task<Void> updateTask = buildVoidSuccessTask();
        when(mockSpecificMsgRef.update(anyString(), any(Object.class))).thenReturn(updateTask);

        ArgumentCaptor<String> fieldCaptor = ArgumentCaptor.forClass(String.class);

        repository.removeReaction(CONV_ID, TEST_MESSAGE_ID, SENDER_ID, mockStatusCallback);

        verify(mockSpecificMsgRef).update(fieldCaptor.capture(), any(Object.class));
        assertEquals("reactions." + SENDER_ID, fieldCaptor.getValue());
        verify(mockStatusCallback).onSuccess();
        verify(mockStatusCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC82: removeReaction — update thất bại → onError(message)
    // -----------------------------------------------------------------------

    @Test
    public void removeReaction_updateFails_callsOnError() {
        Exception exception = new Exception("Network error");
        Task<Void> updateTask = buildVoidFailureTask(exception);
        when(mockSpecificMsgRef.update(anyString(), any(Object.class))).thenReturn(updateTask);

        repository.removeReaction(CONV_ID, TEST_MESSAGE_ID, SENDER_ID, mockStatusCallback);

        verify(mockStatusCallback).onError("Network error");
        verify(mockStatusCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC124: pinMessage — conversationId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void pinMessage_nullConversationId_callsOnError() {
        repository.pinMessage(null, TEST_MESSAGE_ID, SENDER_ID, mockStatusCallback);

        verify(mockStatusCallback).onError("ID hội thoại không hợp lệ.");
        verify(mockStatusCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    // -----------------------------------------------------------------------
    // TC125: pinMessage — messageId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void pinMessage_nullMessageId_callsOnError() {
        repository.pinMessage(CONV_ID, null, SENDER_ID, mockStatusCallback);

        verify(mockStatusCallback).onError("ID tin nhắn không hợp lệ.");
        verify(mockStatusCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    // -----------------------------------------------------------------------
    // TC126: pinMessage — requesterId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void pinMessage_nullRequesterId_callsOnError() {
        repository.pinMessage(CONV_ID, TEST_MESSAGE_ID, null, mockStatusCallback);

        verify(mockStatusCallback).onError("ID người dùng không hợp lệ.");
        verify(mockStatusCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    // -----------------------------------------------------------------------
    // TC127: pinMessage — member doc không tồn tại → onError("Không có quyền ghim tin nhắn.")
    // -----------------------------------------------------------------------

    @Test
    public void pinMessage_memberNotExist_callsOnError() {
        Task<DocumentSnapshot> memberGetTask = buildDocSnapshotSuccessTask(mockMemberSnapshot);
        when(mockSpecificMsgRef.get()).thenReturn(memberGetTask);
        when(mockMemberSnapshot.exists()).thenReturn(false);

        repository.pinMessage(CONV_ID, TEST_MESSAGE_ID, SENDER_ID, mockStatusCallback);

        verify(mockStatusCallback).onError("Không có quyền ghim tin nhắn.");
        verify(mockStatusCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC128: pinMessage — role = "member" → onError("Chỉ admin mới được ghim tin nhắn.")
    // -----------------------------------------------------------------------

    @Test
    public void pinMessage_memberRole_callsOnError() {
        Task<DocumentSnapshot> memberGetTask = buildDocSnapshotSuccessTask(mockMemberSnapshot);
        when(mockSpecificMsgRef.get()).thenReturn(memberGetTask);
        when(mockMemberSnapshot.exists()).thenReturn(true);
        when(mockMemberSnapshot.getString("role")).thenReturn("member");

        repository.pinMessage(CONV_ID, TEST_MESSAGE_ID, SENDER_ID, mockStatusCallback);

        verify(mockStatusCallback).onError("Chỉ admin mới được ghim tin nhắn.");
        verify(mockStatusCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC129: pinMessage — role = "admin" → update isPinned=true, pinnedBy, pinnedAt → onSuccess
    // -----------------------------------------------------------------------

    @Test
    public void pinMessage_adminRole_callsUpdateAndOnSuccess() {
        Task<DocumentSnapshot> memberGetTask = buildDocSnapshotSuccessTask(mockMemberSnapshot);
        when(mockSpecificMsgRef.get()).thenReturn(memberGetTask);
        when(mockMemberSnapshot.exists()).thenReturn(true);
        when(mockMemberSnapshot.getString("role")).thenReturn("admin");
        Task<Void> updateTask = buildVoidSuccessTask();
        when(mockSpecificMsgRef.update(anyMap())).thenReturn(updateTask);

        repository.pinMessage(CONV_ID, TEST_MESSAGE_ID, SENDER_ID, mockStatusCallback);

        verify(mockSpecificMsgRef).update(mapCaptor.capture());
        Map<String, Object> updates = mapCaptor.getValue();
        assertEquals(Boolean.TRUE, updates.get("isPinned"));
        assertEquals(SENDER_ID, updates.get("pinnedBy"));
        assertTrue("Phải có pinnedAt", updates.containsKey("pinnedAt"));
        assertTrue("Phải có updatedAt", updates.containsKey("updatedAt"));
        verify(mockStatusCallback).onSuccess();
        verify(mockStatusCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC130: pinMessage — role = "admin" nhưng update thất bại → onError(message)
    // -----------------------------------------------------------------------

    @Test
    public void pinMessage_adminRole_updateFails_callsOnError() {
        Task<DocumentSnapshot> memberGetTask = buildDocSnapshotSuccessTask(mockMemberSnapshot);
        when(mockSpecificMsgRef.get()).thenReturn(memberGetTask);
        when(mockMemberSnapshot.exists()).thenReturn(true);
        when(mockMemberSnapshot.getString("role")).thenReturn("admin");
        Exception exception = new Exception("Firestore write error");
        Task<Void> updateTask = buildVoidFailureTask(exception);
        when(mockSpecificMsgRef.update(anyMap())).thenReturn(updateTask);

        repository.pinMessage(CONV_ID, TEST_MESSAGE_ID, SENDER_ID, mockStatusCallback);

        verify(mockStatusCallback).onError("Firestore write error");
        verify(mockStatusCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC131: pinMessage — members.get() Firestore thất bại → onError(message)
    // -----------------------------------------------------------------------

    @Test
    public void pinMessage_memberGetFails_callsOnError() {
        Exception exception = new Exception("Firestore read error");
        Task<DocumentSnapshot> memberGetTask = buildDocSnapshotFailureTask(exception);
        when(mockSpecificMsgRef.get()).thenReturn(memberGetTask);

        repository.pinMessage(CONV_ID, TEST_MESSAGE_ID, SENDER_ID, mockStatusCallback);

        verify(mockStatusCallback).onError("Firestore read error");
        verify(mockStatusCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC132: unpinMessage — conversationId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void unpinMessage_nullConversationId_callsOnError() {
        repository.unpinMessage(null, TEST_MESSAGE_ID, SENDER_ID, mockStatusCallback);

        verify(mockStatusCallback).onError("ID hội thoại không hợp lệ.");
        verify(mockStatusCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    // -----------------------------------------------------------------------
    // TC133: unpinMessage — messageId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void unpinMessage_nullMessageId_callsOnError() {
        repository.unpinMessage(CONV_ID, null, SENDER_ID, mockStatusCallback);

        verify(mockStatusCallback).onError("ID tin nhắn không hợp lệ.");
        verify(mockStatusCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    // -----------------------------------------------------------------------
    // TC134: unpinMessage — requesterId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void unpinMessage_nullRequesterId_callsOnError() {
        repository.unpinMessage(CONV_ID, TEST_MESSAGE_ID, null, mockStatusCallback);

        verify(mockStatusCallback).onError("ID người dùng không hợp lệ.");
        verify(mockStatusCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    // -----------------------------------------------------------------------
    // TC135: unpinMessage — role = "member" → onError("Chỉ admin mới được bỏ ghim tin nhắn.")
    // -----------------------------------------------------------------------

    @Test
    public void unpinMessage_memberRole_callsOnError() {
        Task<DocumentSnapshot> memberGetTask = buildDocSnapshotSuccessTask(mockMemberSnapshot);
        when(mockSpecificMsgRef.get()).thenReturn(memberGetTask);
        when(mockMemberSnapshot.exists()).thenReturn(true);
        when(mockMemberSnapshot.getString("role")).thenReturn("member");

        repository.unpinMessage(CONV_ID, TEST_MESSAGE_ID, SENDER_ID, mockStatusCallback);

        verify(mockStatusCallback).onError("Chỉ admin mới được bỏ ghim tin nhắn.");
        verify(mockStatusCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC136: unpinMessage — role = "admin" → update isPinned=false, delete pinnedBy/pinnedAt → onSuccess
    // -----------------------------------------------------------------------

    @Test
    public void unpinMessage_adminRole_callsUpdateAndOnSuccess() {
        Task<DocumentSnapshot> memberGetTask = buildDocSnapshotSuccessTask(mockMemberSnapshot);
        when(mockSpecificMsgRef.get()).thenReturn(memberGetTask);
        when(mockMemberSnapshot.exists()).thenReturn(true);
        when(mockMemberSnapshot.getString("role")).thenReturn("admin");
        Task<Void> updateTask = buildVoidSuccessTask();
        when(mockSpecificMsgRef.update(anyMap())).thenReturn(updateTask);

        repository.unpinMessage(CONV_ID, TEST_MESSAGE_ID, SENDER_ID, mockStatusCallback);

        verify(mockSpecificMsgRef).update(mapCaptor.capture());
        Map<String, Object> updates = mapCaptor.getValue();
        assertEquals(Boolean.FALSE, updates.get("isPinned"));
        assertTrue("Phải có pinnedBy (FieldValue.delete)", updates.containsKey("pinnedBy"));
        assertTrue("Phải có pinnedAt (FieldValue.delete)", updates.containsKey("pinnedAt"));
        assertTrue("Phải có updatedAt", updates.containsKey("updatedAt"));
        verify(mockStatusCallback).onSuccess();
        verify(mockStatusCallback, never()).onError(anyString());
    }
}
