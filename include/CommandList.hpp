// Copyright (c) 2010 Ryan Seal <rlseal -at- gmail.com>
//
// This file is part of GnuRadar Software.
//
// GnuRadar is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// GnuRadar is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with GnuRadar.  If not, see <http://www.gnu.org/licenses/>.
#ifndef COMMAND_LIST_HPP
#define COMMAND_LIST_HPP

#include <iostream>
#include <vector>
#include <stdexcept>

#include <boost/shared_ptr.hpp>

#include <GnuRadarCommand.hpp>

namespace gnuradar {
	namespace command {

		typedef boost::shared_ptr< command::GnuRadarCommand > CommandPtr;

		struct CommandList {

			typedef std::vector< CommandPtr > CommandVector;
			CommandVector commands_;

			public:
			CommandList() {}

			void Add ( CommandPtr commandPtr ) {
				commands_.push_back ( commandPtr );
			}

			const CommandPtr Find ( const std::string& name ) {
				CommandVector::const_iterator iter = commands_.begin();

				while ( iter != commands_.end() ) {
					if ( name == ( *iter )->Name() ) break;
					++iter;
				}

				// check for an invalid command and throw
				if ( iter == commands_.end() )
					throw std::runtime_error ( "Invalid command request" );

				return *iter;
			}
		};

	};
};

#endif
