using System;

class GuessingGame
{
    static void Main()
    {
        // User 1 enters the secret number
        int secret = ReadNumber("User 1, enter a secret number (1-100): ");
        Console.Clear(); // hide the number from User 2

        // User 2 guesses
        int guesses = 0;
        int guess;
        do
        {
            guess = ReadNumber("User 2, enter your guess (1-100): ");
            guesses++;

            if (guess > secret)
                Console.WriteLine("Too high!");
            else if (guess < secret)
                Console.WriteLine("Too low!");
        } while (guess != secret);

        Console.WriteLine($"Correct! The number was {secret}. It took {guesses} guess(es).");

        // Decide the winner
        if (guesses <= 5)
            Console.WriteLine("User 2 wins!");
        else
            Console.WriteLine("User 1 wins!");
    }

    // Keeps asking until the user enters a whole number from 1 to 100
    static int ReadNumber(string prompt)
    {
        while (true)
        {
            Console.Write(prompt);
            if (int.TryParse(Console.ReadLine(), out int n) && n >= 1 && n <= 100)
                return n;
            Console.WriteLine("Please enter a whole number from 1 to 100.");
        }
    }
}
