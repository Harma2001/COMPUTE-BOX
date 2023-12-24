package com.yanamobility.usblibrary.Utils;

/**
 * Created by VIMAL on 15-10-2017.
 */

public class DataConversion
{
    public static void main(String[] args)
    {

        byte[] res = _ByteToPAN((byte) 0x1A);
        System.out.println("_ByteToPAN : " + new String(res).length());

        byte[] res1 = _HWordToPAN((short) 0x1A3B);
        System.out.println("_HWordToPAN : " + new String(res1));

        byte[] res2 = _WordToPAN((int) 0x1A3B5C7D);
        System.out.println("_WordToPAN : " + new String(res2));

        byte[] res3 = _DwordToPAN(0x1A3B5C7D1E3F5A7Bl);
        System.out.println("_DwordToPAN : " + new String(res3));
        System.out.println("-----------------------------------------------------------------");


        //byte[] res2= _HWordToPAN((short)0xFACB);
        //byte[] res2 = _WordToPAN((int)0x12345678);
        byte[] arr1 = {'A', '2'};
        byte[] arr2 = {'A', 'B', '3', '4'};
        byte[] arr3 = {'A', 'B', 'C', 'D', '5', '6', '7', '8'};
        byte[] arr4 = {'A', 'B', 'C', 'D', '5', '6', '7', '8', 'A', 'A', 'B', 'B', 'C', 'C', 'D', 'D'};
        byte[] result = new byte[8];

        byte b = _PanToByte(arr1);
        //System.out.println("_PanToByte : "+Integer.toHexString((int)b));
        short b1 = _PanToHWord(arr2);
        //System.out.println("_PanToHWord : "+Integer.toHexString((int)b1));
        int b3 = _PanToWord(arr3);
        System.out.println("_PanToWord : " + Integer.toHexString((int) b3));
        long b4 = _PanToDWord(arr4);
        System.out.println("_PanToDWord : " + Long.toHexString(b4));
        //long res = _PanToDWord(arr4);
        result = _PanToByteArray(arr4, 8);
        for (int i = 0; i < result.length; i++) {
            System.out.println(result[i]);
        }

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

    public static byte [] _WordToPAN(int Input)
    {
		 /* */
        byte[] lTemp = new byte[8];

		 /* split bytes */
        short LowHWord  = (short) (Input & 0x0000FFFF);
        short HighHWord = (short) ((Input & 0xFFFF0000) >> 16);

        byte[] v1 =  _HWordToPAN(LowHWord);
        byte[] v2 =  _HWordToPAN(HighHWord);


        System.arraycopy(v2, 0, lTemp, 0, 4);
        System.arraycopy(v1, 0, lTemp, 4, 4);

		/* return char array */
        return lTemp;
    }

    public static byte [] _DwordToPAN(long Input)
    {
		 /* */
        byte[] lTemp = new byte[16];

		 /* split bytes */
        int LowWord  = (int) (Input & 0x00000000FFFFFFFFl);
        int HighWord = (int) ((Input & 0xFFFFFFFF00000000l) >> 32);

        byte[] v1 =  _WordToPAN(LowWord);
        byte[] v2 =  _WordToPAN(HighWord);


        System.arraycopy(v2, 0, lTemp, 0, 8);
        System.arraycopy(v1, 0, lTemp, 8, 8);

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


    public static int _PanToWord(byte[] Input)
    {
        long Output = 0;
        short LowWord = 0;
        short HighWord = 0;

        byte[] LHWord = new byte[4];
        System.arraycopy(Input, 4, LHWord, 0, 4);
        byte[] HHWord = new byte[4];
        System.arraycopy(Input, 0, HHWord, 0, 4);

		 /* */
        LowWord = _PanToHWord(LHWord);

        HighWord = _PanToHWord(HHWord);

        Output = (int)((HighWord & 0x0000FFFF) << 16) | (LowWord & 0x0000FFFF);

        return (int)Output;
    }

    public static long _PanToDWord(byte[] Input)
    {
        Long Output = (long) 0;
        long LowWord = 0;
        long HighWord = 0;

        byte[] LWord = new byte[8];
        System.arraycopy(Input, 8, LWord, 0, 8);
        byte[] HWord = new byte[8];
        System.arraycopy(Input, 0, HWord, 0, 8);

		 /* */
        LowWord = _PanToWord(LWord);
        HighWord = _PanToWord(HWord);

        Output = (long)((long)(HighWord & 0x00000000FFFFFFFFL) << 32) | (long)(LowWord & 0x00000000FFFFFFFFL);

        return Output;
    }
    public static byte[] _PanToByteArray(byte[] SrcBuffer, int DestLength)
    {
		 /* */
        byte[] Output = new byte[DestLength];

		 /* */
        byte[] tempbuf = new byte[2];

		 /* */
        int Index = 0;
        int Index1 = 0;

        for(Index = 0; Index < DestLength; Index++)
        {
			 /* */
            System.arraycopy(SrcBuffer, Index1, tempbuf, 0, 2);

			 /* */
            Output[Index] = _PanToByte(tempbuf);

			 /* */
            Index1 = Index1+2;
        }

		 /* */
        return Output;
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
    public static int GenLength(byte[] input)
    {
        int len = 0;
        for(int index = 0;index<input.length; index++)
        {
            if(0x00 != input[index])
            {
                len = len+1;
            }
            else
            {
                break;
            }
        }
        return len;
    }

    public static boolean ValidateHexByteArray(byte[] input,int length)
    {
        /* To hold result*/
        boolean Result = true;

        int Index = 0;

        for(Index = 0; Index < length ; Index++)
        {
            if(((input[Index] >= 48) && (input[Index] <= 57)) || ((input[Index] >= 65) && (input[Index] <= 70)))
            {
                /* Do Nothing */
            }
            else
            {
                if((Index == 4) && ((input[Index] == 0x06) || (input[Index] == 0x15)))
                {
                    /* Do Nothing */
                }
                else
                {
                    /* Update Result to False */
                    Result = false;

                /* Break the Loop */
                    break;
                }
            }
        }

        /* return the result */
        return Result;
    }

    public static int SearchChar(byte[] input, int length, char InputChar)
    {
        /* Result */
        int Result = 0;
        int Index = 0;
        boolean FoundFlag = false;

        for(Index = 0; Index < length; Index++)
        {
            if(InputChar == input[Index])
            {
                /* Update the Flag Status as Found */
                FoundFlag = true;

                /* break the loop */
                break;
            }
        }

        if(FoundFlag == true)
        {
            /* update the Return Index */
            Result = Index;
        }
        else
        {
            /* Update Negative Index to Indicate False Status */
            Result = -1;
        }

        return Result;
    }
    private final static  char[] hexArray="0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes)
    {

        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ )
        {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    public static byte[] hexStringToByteArray(String s)
    {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
        {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
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
