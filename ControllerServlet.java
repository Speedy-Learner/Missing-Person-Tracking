
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.GetLabelDetectionRequest;
import com.amazonaws.services.rekognition.model.GetLabelDetectionResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.rekognition.model.LabelDetection;
import com.amazonaws.services.rekognition.model.LabelDetectionSortBy;
import com.amazonaws.services.rekognition.model.NotificationChannel;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.rekognition.model.StartLabelDetectionRequest;
import com.amazonaws.services.rekognition.model.StartLabelDetectionResult;
import com.amazonaws.services.rekognition.model.Video;
import com.amazonaws.services.rekognition.model.VideoMetadata;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Servlet implementation class ControllerServlet
 */
@WebServlet("/ControllerServlet")
public class ControllerServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String bucket = "s3bucketreko";
	private static AWSCredentials credentials;
	private static String filePath = "C:\\Users\\Santosh Pethe\\Desktop\\Spartahack\\images\\";
	private static String topicARN = "arn:aws:sns:us-west-2:487647996021:MyTopic";
	private static String roleARN = "arn:aws:iam::487647996021:role/AmazonRekognitionServiceRole";
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ControllerServlet() {
		super();
		try {
			credentials = new ProfileCredentialsProvider("default").getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
					+ "Please make sure that your credentials file is at the correct "
					+ "location (/Users/userid/.aws/credentials), and is in a valid format.", e);
		}
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		System.out.println("im in server");
		String imgName = request.getParameter("imgFile");
		String videoName = request.getParameter("videoFile");
		String[] imgPathNames = new String[2];
		String keyName = "";

		imgPathNames[0] = imgName;
		imgPathNames[1] = videoName;
		try {
			videoDetect(videoName);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (String imgPath : imgPathNames) {
			keyName = imgPath;
			System.out.println("file uploaded" + imgPath);
			// uploadObjectsToS3(keyName, filePath+imgPath);
		}

		// detectLabelsInImage(inputImg);
		PrintWriter writer = response.getWriter();

		// build HTML code
		String htmlRespone = "<html>";
		htmlRespone += "<h2> Success </h2>";
		htmlRespone += "</html>";

		response.sendRedirect("index.html");

	}

	@SuppressWarnings("unused")
	private void uploadObjectsToS3(String keyName, String uploadFileName) {
		// String bucketName = "*** Provide bucket name ***";
		// String uploadFileName = "*** Provide file name ***";
		AmazonS3 s3client = new AmazonS3Client(credentials);
		try {
			System.out.println("Uploading a new object to S3 from a file\n");
			File file = new File(uploadFileName);
			s3client.putObject(new PutObjectRequest(bucket, keyName, file));

		} catch (AmazonServiceException ase) {
			System.out.println("Caught an AmazonServiceException, which " + "means your request made it "
					+ "to Amazon S3, but was rejected with an error response" + " for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			System.out.println("Caught an AmazonClientException, which " + "means the client encountered "
					+ "an internal error while trying to " + "communicate with S3, "
					+ "such as not being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
		}
	}

	private void detectLabelsInImage(String inputImg) {
		String photo = inputImg;
		AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard().withRegion(Regions.US_WEST_2)
				.withCredentials(new AWSStaticCredentialsProvider(credentials)).build();
		DetectLabelsRequest request = new DetectLabelsRequest()
				.withImage(new Image().withS3Object(new S3Object().withName(photo).withBucket(bucket)))
				.withMaxLabels(10).withMinConfidence(75F);
		try {
			DetectLabelsResult result = rekognitionClient.detectLabels(request);
			List<Label> labels = result.getLabels();
			System.out.println("Detected labels for " + photo);
			for (Label label : labels) {
				System.out.println(label.getName() + ": " + label.getConfidence().toString());
			}
		} catch (AmazonRekognitionException e) {
			e.printStackTrace();
		}
	}

	private static AmazonSNS sns = null;
	private static AmazonSQS sqs = null;
	private static AmazonRekognition rek = null;
	private static NotificationChannel channel = new NotificationChannel().withSNSTopicArn(topicARN)
			.withRoleArn(roleARN);
	private static String queueUrl = "QueueURL";
	private static String startJobId = null;

	public void videoDetect(String videoName) throws Exception{
		 try {
		 credentials = new ProfileCredentialsProvider("default").getCredentials();
		 } catch (Exception e) { 
			 throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
					 + "Please make sure that your credentials file is at the correct "
					 + "location (/Users/userid>.aws/credentials), and is in valid format.",
					 e);
		}
		 sns = AmazonSNSClientBuilder
		 .standard()
		 .withRegion(Regions.US_EAST_2)
		 .withCredentials(new AWSStaticCredentialsProvider(credentials))
		 .build();
		 sqs = AmazonSQSClientBuilder
		 .standard()
		 .withRegion(Regions.US_EAST_2)
		 .withCredentials(new AWSStaticCredentialsProvider(credentials))
		 .build(); 
		 rek = AmazonRekognitionClientBuilder.standard().withCredentials( new
				 ProfileCredentialsProvider("default"))
				 .withEndpointConfiguration(new EndpointConfiguration("https://sns.us-west-2.amazonaws.com",
				 "us-west-2")).build();
		 //=================================================
		 StartLabels(bucket, videoName);
		 //=================================================
		 System.out.println("Waiting for job: " + startJobId);
		 //Poll queue for messages
		 List<Message> messages=null;
		 int dotLine=0;
		 boolean jobFound=false;
		 //loop until the job status is published. Ignore other messages in queue.
		 do{
		 //Get messages.
			 do{
				 messages = sqs.receiveMessage(queueUrl).getMessages();
				 if (dotLine++<20){
					 System.out.print(".");
				 }else{
					 System.out.println();
					 dotLine=0;
				 }
			 }while(messages.isEmpty());
		 System.out.println();
		 //Loop through messages received.
		 for (Message message: messages) {
			 String notification = message.getBody();
			 // Get status and job id from notification.
			 ObjectMapper mapper = new ObjectMapper();
			 JsonNode jsonMessageTree = mapper.readTree(notification);
			 JsonNode messageBodyText = jsonMessageTree.get("Message");
			 ObjectMapper operationResultMapper = new ObjectMapper();
			 JsonNode jsonResultTree =
			 operationResultMapper.readTree(messageBodyText.textValue());
			 JsonNode operationJobId = jsonResultTree.get("JobId");
			 JsonNode operationStatus = jsonResultTree.get("Status");
			 System.out.println("Job found was " + operationJobId);
			 // Found job. Get the results and display.
			 if(operationJobId.asText().equals(startJobId)){ 
					 jobFound=true;
					 System.out.println("Job id: " + operationJobId );
					 System.out.println("Status : " + operationStatus.toString());
					 if (operationStatus.asText().equals("SUCCEEDED")){
					 //============================================
						 GetResultsLabels();
						 //============================================
					  }
					  else{
					      System.out.println("Video analysis failed");
					  }
					 sqs.deleteMessage(queueUrl,message.getReceiptHandle());
			 }
			 else{
				 System.out.println("Job received was not job " + startJobId);
			 }
		  }
		} while (!jobFound);
		System.out.println("Done!");
}

	private static void StartLabels(String bucket, String video) throws Exception {

		/*StartFaceDetectionRequest req = new StartFaceDetectionRequest()
				.withVideo(new Video().withS3Object(new S3Object().withBucket(bucket).withName(video).withVersion("1")))
				.withFaceAttributes("ALL")
				.withNotificationChannel(channel);

		StartFaceDetectionResult startLabelDetectionResult = rek.startFaceDetection(req);
		startJobId = startLabelDetectionResult.getJobId();
*/
		StartLabelDetectionRequest req = new StartLabelDetectionRequest()
				 .withVideo(new Video()
				 .withS3Object(new S3Object()
				 .withBucket(bucket)
				 .withName(video)))
				 .withMinConfidence(50F)
				 .withJobTag("DetectingLabels")
				 .withNotificationChannel(channel);
		System.out.println(req);
				 StartLabelDetectionResult startLabelDetectionResult = rek.startLabelDetection(req);
				 System.out.println(startLabelDetectionResult);
				 //startJobId=startLabelDetectionResult.getJobId(); 
	}

	private static void GetResultsLabels() throws Exception {

		/*int maxResults = 10;
		String paginationToken = null;
		GetFaceDetectionResult faceDetectionResult = null;

		do {
			if (faceDetectionResult != null) {
				paginationToken = faceDetectionResult.getNextToken();
			}

			faceDetectionResult = rek.getFaceDetection(new GetFaceDetectionRequest().withJobId(startJobId)
					.withNextToken(paginationToken).withMaxResults(maxResults));

			VideoMetadata videoMetaData = faceDetectionResult.getVideoMetadata();

			System.out.println("Format: " + videoMetaData.getFormat());
			System.out.println("Codec: " + videoMetaData.getCodec());
			System.out.println("Duration: " + videoMetaData.getDurationMillis());
			System.out.println("FrameRate: " + videoMetaData.getFrameRate());

			// Show faces, confidence and detection times
			List<FaceDetection> faces = faceDetectionResult.getFaces();

			for (FaceDetection face : faces) {
				long seconds = face.getTimestamp() / 1000;
				System.out.print("Sec: " + Long.toString(seconds) + " ");
				System.out.println(face.getFace().toString());
				System.out.println();
			}
		} while (faceDetectionResult != null && faceDetectionResult.getNextToken() != null);

	}*/
		int maxResults=10;
		 String paginationToken=null;
		 GetLabelDetectionResult labelDetectionResult=null;
		 do {
		 if (labelDetectionResult !=null){
		 paginationToken = labelDetectionResult.getNextToken();
		 }
		 GetLabelDetectionRequest labelDetectionRequest= new
		 GetLabelDetectionRequest()
		 .withJobId(startJobId)
		 .withSortBy(LabelDetectionSortBy.TIMESTAMP)
		 .withMaxResults(maxResults)
		 .withNextToken(paginationToken);
		 labelDetectionResult = rek.getLabelDetection(labelDetectionRequest);
		 VideoMetadata videoMetaData=labelDetectionResult.getVideoMetadata(); 
		 System.out.println("Format: " + videoMetaData.getFormat());
		 System.out.println("Codec: " + videoMetaData.getCodec());
		 System.out.println("Duration: " + videoMetaData.getDurationMillis());
		 System.out.println("FrameRate: " + videoMetaData.getFrameRate());
		 //Show labels, confidence and detection times
		 List<LabelDetection> detectedLabels= labelDetectionResult.getLabels();
		 for (LabelDetection detectedLabel: detectedLabels) {
		 long seconds=detectedLabel.getTimestamp()/1000;
		 System.out.print("Sec: " + Long.toString(seconds) + " ");
		 System.out.println("\t" + detectedLabel.getLabel().getName() +
		 " \t" +
		 detectedLabel.getLabel().getConfidence().toString());
		 System.out.println();
		 }
		 } while (labelDetectionResult !=null && labelDetectionResult.getNextToken() !=
		 null);
		 } 
	
	public static void main(String[] args) {
		System.out.println("inside");
		try {
			AmazonRekognition client = AmazonRekognitionClientBuilder.standard().withCredentials( new
					 ProfileCredentialsProvider("default"))
					 .withEndpointConfiguration(new EndpointConfiguration("https://sns.us-west-2.amazonaws.com",
					 "us-west-2")).build();
			//StartLabels(bucket, "VID-20161128-WA0007.mp4");
			//AmazonRekognition client = AmazonRekognitionClientBuilder.standard().build();
			DetectLabelsRequest request = new DetectLabelsRequest().withImage(new Image().withS3Object(new S3Object().withBucket(bucket).withName("VID-20161128-WA0007.mp4")))
			        .withMaxLabels(123).withMinConfidence(70f);
			DetectLabelsResult response = client.detectLabels(request);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("done");
	}

}
