//
// client.cpp
// ~~~~~~~~~~
//
// Copyright (c) 2003-2008 Christopher M. Kohlhoff (chris at kohlhoff dot com)
//
// Distributed under the Boost Software License, Version 1.0. (See accompanying
// file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//

#include <iostream>
#include <boost/array.hpp>
#include <boost/asio.hpp>

using boost::asio::ip::tcp;

void handle_write(const boost::system::error_code& /*error*/,
      size_t /*bytes_transferred*/)
{
}

int main(int argc, char* argv[])
{
  try
  {
    if (argc != 2)
    {
      std::cerr << "Usage: client <host>" << std::endl;
      return 1;
    }

    boost::asio::io_service io_service;

    tcp::resolver resolver(io_service);
    tcp::resolver::query query(argv[1], "gnuradar");
    tcp::resolver::iterator endpoint_iterator = resolver.resolve(query);
    tcp::resolver::iterator end;

    tcp::socket socket(io_service);
    boost::system::error_code error = boost::asio::error::host_not_found;
    while (error && endpoint_iterator != end)
    {
       socket.close();
       socket.connect(*endpoint_iterator++, error);
    }
    if (error)
       throw boost::system::system_error(error);

    for (;;)
    {
       boost::array<char, 512> buf;
       boost::system::error_code error;

       socket.async_write_some( boost::asio::buffer("health"), handle_write);

       size_t len = socket.read_some(boost::asio::buffer(buf), error);

       if (error == boost::asio::error::eof)
          break; // Connection closed cleanly by peer.
       else if (error)
          throw boost::system::system_error(error); // Some other error.

       std::string result = buf.data();
       std::cout << result.substr(0,len);
    }
  }
  catch (std::exception& e)
  {
     std::cerr << e.what() << std::endl;
  }

  return 0;
}

