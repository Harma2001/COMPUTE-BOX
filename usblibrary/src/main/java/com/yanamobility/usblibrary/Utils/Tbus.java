package com.yanamobility.usblibrary.Utils;

public class Tbus
{

	/*Tbus is nothing but a frameformat we used to send and recieve data in a way*/
	public static short currentChecksum=0;
	/*public static void main(String args[])
	{
		byte[] payload="1234".getBytes();

		byte [] response = Command((byte)0x41,(byte)0x00,null,(short)0);
		System.out.print(new String(response));

		response = parseResponse(response);

		System.out.print(new String(response));
	}*/
	public static byte[] formCommand(byte sid,byte did, byte[] payLoad ,short payloadLen)
	{
		
		byte[] tbusPacket =new byte[payloadLen + 8 +7];
		byte[] tbusLengthBuff = new byte[4];
		byte[] tbusChecksumBuff = new byte[4];
		byte[] tempBuff = new byte[payloadLen + 8];
		byte[] tempBuff1 = new byte[2];
		
		
		tbusLengthBuff = _HWordToPAN((short) (payloadLen + 8)) ;
		System.arraycopy(tbusLengthBuff, 0, tempBuff, 0, 4);
		
		tempBuff1 = _ByteToPAN(sid);
		System.arraycopy(tempBuff1, 0, tempBuff, 4, 2);
		
		tempBuff1 = _ByteToPAN(did);
		System.arraycopy(tempBuff1, 0, tempBuff, 6, 2);
		if(payloadLen>0)
		{
			System.arraycopy(payLoad, 0, tempBuff, 8, payloadLen);
		}
		else
		{
			
		}
		short checkSum= GetCheckSum(tempBuff,tempBuff.length);
		currentChecksum= checkSum;
		tbusChecksumBuff=_HWordToPAN(checkSum) ;
		
		
		tbusPacket[0] =':';/*Added tbus :*/
		System.arraycopy(tempBuff, 0, tbusPacket, 1, tempBuff.length);/*Added tbus Length + tbus siddid+ data*/
		
		System.arraycopy(tbusChecksumBuff, 0, tbusPacket, 1+tempBuff.length, 4);/*Added tbus CheckSum*/
		
		System.arraycopy("\r\n".getBytes(), 0, tbusPacket, 1+tempBuff.length+4, 2);/*Added tbus \r\n*/
		
		return tbusPacket;
		
	}
	
	public static byte[] parseResponse(byte[] response)
	{
		if(response[0] == ':')
		{
			byte[] tempBuffer = new byte[4];
			System.arraycopy(response, 1, tempBuffer, 0, 4);
			
			short length = _PanToHWord(tempBuffer);
			tempBuffer = new byte[length];
			
			System.arraycopy(response, 1, tempBuffer, 0, tempBuffer.length);
			
			short checkSum = GetCheckSum(tempBuffer,tempBuffer.length);
			tempBuffer = new byte[4];
			System.arraycopy(response, length + 1 , tempBuffer, 0, 4);
			
			short recievedChecksum = _PanToHWord(tempBuffer);;
			if(recievedChecksum == checkSum)
			{
				if((length-8) > 0) {
					byte[] payload = new byte[length - 8];
					System.arraycopy(response, 9, payload, 0, length - 8);
					return payload;
				}
				else
				{
					return null;
				}
			}
			else
			{
				return null;
			}
		}
		else
		{
			return null;
		}
	}


	
	 public static byte [] _HWordToPAN(short Input)
	{
		 /* */
		byte[] lTemp = new byte[4];

		 /* split bytes */
		byte LowByte  = (byte) (Input & 0x00FF);
		byte HighByte = (byte) ((Input & 0xFF00) >> 8);

		byte[] v1 =  _ByteToPAN(LowByte);
		byte[] v2 =  _ByteToPAN(HighByte);


		System.arraycopy(v2, 0, lTemp, 0, 2);
		System.arraycopy(v1, 0, lTemp, 2, 2);

		/* return char array */
		return lTemp;
	}
	 
	  public static byte [] _ByteToPAN(byte Input)
	{

		byte[] lTemp = new byte[2];


		byte LowerNibble = (byte) (Input & 0x0F);
		byte HighNibble = (byte)((Input & 0xF0) >> 4);

		if((HighNibble >= 0) && (HighNibble <= 9))
		{
			lTemp[0] = (byte)(48 + HighNibble);
		}
		else if((HighNibble >= 10) && (HighNibble <= 15))
		{
			lTemp[0] = (byte)(55 + HighNibble);
		}
		else
		{

		}

		if((LowerNibble >= 0) && (LowerNibble <= 9))
		{
			lTemp[1] = (byte)(48 + LowerNibble);
		}
		else if((LowerNibble >= 10) && (LowerNibble <= 15))
		{
			lTemp[1] = (byte)(55 + LowerNibble);
		}
		else
		{

		}

		 /* return char array */
		return lTemp;
	}
	  
	  public static byte _PanToByte(byte[] Input)
		{
		byte Output = 0;

		byte LowNibble = Input[1];
		byte HighNibble = Input[0];

		if((LowNibble >= '0') && (LowNibble <= '9'))
		{
			 /* */
			LowNibble = (byte)(LowNibble - 48);
		}
		else if((LowNibble >= 'A') && (LowNibble <= 'F'))
		{
			 /* */
			LowNibble = (byte)(LowNibble - 55);
		}
		else
		{

		}

		if((HighNibble >= '0') && (HighNibble <= '9'))
		{
			 /* */
			HighNibble = (byte)(HighNibble - 48);
		}
		else if((HighNibble >= 'A') && (HighNibble <= 'F'))
		{
			 /* */
			HighNibble = (byte)(HighNibble - 55);
		}
		else
		{

		}

		Output = (byte)(((HighNibble & 0x0F) << 4) | (LowNibble & 0x0F));

		return Output;
		}



	    public static short _PanToHWord(byte[] Input)
	    {
	        int Output = 0;
	        byte LowByte = 0;
	        byte HighByte = 0;

	        byte[] LByte = new byte[2];
	        System.arraycopy(Input, 2, LByte, 0, 2);
	        byte[] HByte = new byte[2];
	        System.arraycopy(Input, 0, HByte, 0, 2);

			 /* */
	        LowByte = _PanToByte(LByte);
	        HighByte = _PanToByte(HByte);

	        Output = (short)(((HighByte & 0x00FF) << 8) | (LowByte & 0x00FF));

	        Output = Output & 0x0000FFFF;
	        return (short)Output;
	    }

	  
  public static short GetCheckSum(byte[] Input, int length)
	{
		/* To Hold Checksum */
		short CheckSum = 0;

		/* to hold Sum */
		int Sum = 0;

		 /* Temporary variable to perform 16 bit operations */
		short Temp1 = 0;

		 /* Temporary variable to perform 8 bit operations */
		byte Temp2 = 0;
		byte Temp3 = 0;

		 /* Calculate the sum of bytes */
		for(int Index = 0; Index < length; Index++)
		{
			Sum = Sum + Input[Index];
		}

		 /* */
		Temp1 = (short) (Sum & 0x0000FFFF);

		 /* Hold 2's Compliments */
		Temp1 = (short) ((~Temp1) + 1);

		 /* */
		Temp2 = (byte)(Temp1 & 0x00FF);

		 /* */
		CheckSum = (short)Temp2;

		 /* Left shifting by 8*/
		CheckSum = (short) (CheckSum << 8);

		 /* */
		Temp3 = (byte)(~Temp2);

		 /* */
		CheckSum |= (short)(0x00FF & Temp3);

		/* */
		return CheckSum;

	}

	public static byte[] _ByteArrayToPanArray(byte[] arra)
	{
		byte[] output = new byte[arra.length * 2];
		byte[] tempBuffer =new byte[2];
		for(int i=0 , j=0 ;i < arra.length ; i++, j= j+2) {
			tempBuffer = DataConversion._ByteToPAN(arra[i]);
			output[j] = tempBuffer[0];
			output[j + 1] = tempBuffer[1];
		}
		return output;
	}

}
